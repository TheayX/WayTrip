#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const SERVER_ROOT = path.resolve(__dirname, "..");
const DB_ROOT = path.join(SERVER_ROOT, "src", "main", "resources", "db");
const DEFAULT_OUTPUT_DIR = path.join(DB_ROOT, "seed", "bulk");

const USER_PASSWORD_HASH = "$2a$10$kNs.tGrq9fm.h/4yF51JUe9DGyC1Jb8nTt9KYsFHBybPvmqBqfoOm";
const CATEGORY_IDS = [7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20];
const VIEW_SOURCES = ["home", "search", "recommendation", "guide", "detail"];
const SURNAMES = ["陈", "林", "周", "徐", "何", "高", "梁", "宋", "谢", "唐", "许", "韩", "冯", "邓", "曹", "彭", "曾", "蒋", "田", "杜"];
const GIVEN_NAMES = ["知远", "清和", "沐言", "若溪", "星野", "安禾", "景川", "书宁", "可言", "屿白", "云舒", "亦南", "听澜", "时安", "初晴", "嘉木", "怀青", "映夏", "言蹊", "知夏"];

const DEFAULTS = {
  userCount: 120,
  startUserId: 10001,
  startPreferenceId: 20001,
  startFavoriteId: 30001,
  startViewId: 40001,
  startReviewId: 50001,
  startOrderId: 60001,
  startOrderNoSeq: 1,
  seed: 20260514,
  outputDir: DEFAULT_OUTPUT_DIR
};

async function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.help) {
    printHelp();
    return;
  }

  const config = buildConfig(args);
  validateConfig(config);

  const spotPool = await loadSpotPool(config);
  if (!spotPool.length) {
    throw new Error("没有可用景点数据。请先准备 db/data.sql，或先生成 bulk/10_spot.sql。");
  }

  const rng = createRng(config.seed);
  const users = buildUsers(config, rng);
  const datasets = buildDatasets(users, spotPool, config, rng);

  await fs.mkdir(config.outputDir, { recursive: true });
  await writeSqlFiles(config.outputDir, datasets);

  console.log(`已生成 ${users.length} 个用户的扩容 SQL 到 ${config.outputDir}`);
}

function printHelp() {
  console.log(`用法:
  node travel-server/scripts/generate-bulk-behavior-sql.mjs [options]

选项:
  --user-count <n>             生成用户数量，默认 120
  --start-user-id <n>          用户起始 ID，默认 10001
  --start-preference-id <n>    偏好起始 ID，默认 20001
  --start-favorite-id <n>      收藏起始 ID，默认 30001
  --start-view-id <n>          浏览起始 ID，默认 40001
  --start-review-id <n>        评论起始 ID，默认 50001
  --start-order-id <n>         订单起始 ID，默认 60001
  --start-order-no-seq <n>     订单流水起始序号，默认 1
  --seed <n>                   随机种子，默认 20260514
  --output-dir <path>          输出目录，默认 src/main/resources/db/seed/bulk
  --help                       查看帮助
`);
}

function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (token === "--help") {
      args.help = true;
      continue;
    }
    if (!token.startsWith("--")) {
      throw new Error(`无法识别的参数: ${token}`);
    }
    const key = token.slice(2);
    const value = argv[i + 1];
    if (value == null || value.startsWith("--")) {
      throw new Error(`参数缺少值: ${token}`);
    }
    args[key] = value;
    i += 1;
  }
  return args;
}

function buildConfig(args) {
  return {
    userCount: parseInteger(args["user-count"], DEFAULTS.userCount),
    startUserId: parseInteger(args["start-user-id"], DEFAULTS.startUserId),
    startPreferenceId: parseInteger(args["start-preference-id"], DEFAULTS.startPreferenceId),
    startFavoriteId: parseInteger(args["start-favorite-id"], DEFAULTS.startFavoriteId),
    startViewId: parseInteger(args["start-view-id"], DEFAULTS.startViewId),
    startReviewId: parseInteger(args["start-review-id"], DEFAULTS.startReviewId),
    startOrderId: parseInteger(args["start-order-id"], DEFAULTS.startOrderId),
    startOrderNoSeq: parseInteger(args["start-order-no-seq"], DEFAULTS.startOrderNoSeq),
    seed: parseInteger(args.seed, DEFAULTS.seed),
    outputDir: args["output-dir"]
      ? path.resolve(process.cwd(), args["output-dir"])
      : DEFAULTS.outputDir
  };
}

function validateConfig(config) {
  if (config.userCount < 1 || config.userCount > 1000) {
    throw new Error("--user-count 必须在 1 到 1000 之间。");
  }
  const starts = [
    config.startUserId,
    config.startPreferenceId,
    config.startFavoriteId,
    config.startViewId,
    config.startReviewId,
    config.startOrderId,
    config.startOrderNoSeq
  ];
  if (starts.some((value) => value < 1)) {
    throw new Error("所有起始 ID 和流水号都必须大于 0。");
  }
}

async function loadSpotPool(config) {
  const files = [
    path.join(DB_ROOT, "data.sql"),
    path.join(config.outputDir, "10_spot.sql")
  ];
  const spotMap = new Map();

  for (const file of files) {
    try {
      const content = await fs.readFile(file, "utf8");
      for (const spot of parseSpotRows(content)) {
        spotMap.set(spot.id, spot);
      }
    } catch (error) {
      if (error.code !== "ENOENT") {
        throw error;
      }
    }
  }

  return Array.from(spotMap.values()).sort((left, right) => left.id - right.id);
}

function parseSpotRows(sql) {
  const rows = [];
  const lines = String(sql).split(/\r?\n/);
  let inSpotInsert = false;
  let inValues = false;

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) {
      continue;
    }
    if (line.startsWith("INSERT INTO `spot`")) {
      inSpotInsert = true;
      inValues = false;
      continue;
    }
    if (!inSpotInsert) {
      continue;
    }
    if (line.startsWith("VALUES")) {
      inValues = true;
      continue;
    }
    if (!inValues || !line.startsWith("(")) {
      continue;
    }

    const tuple = parseSqlTuple(line);
    if (tuple.length < 15) {
      continue;
    }

    rows.push({
      id: Number(tuple[0]),
      name: tuple[1],
      price: Number(tuple[3]),
      categoryId: Number(tuple[9]),
      regionId: Number(tuple[10]),
      heatScore: Number(tuple[12] ?? 0)
    });

    if (line.endsWith(";")) {
      inSpotInsert = false;
      inValues = false;
    }
  }

  return rows.filter((spot) => Number.isFinite(spot.id));
}

function parseSqlTuple(line) {
  const cleaned = line.replace(/^[\s(]+/, "").replace(/[),;\s]+$/, "");
  const values = [];
  let current = "";
  let inString = false;

  for (let index = 0; index < cleaned.length; index += 1) {
    const char = cleaned[index];
    const next = cleaned[index + 1];

    if (char === "'") {
      if (inString && next === "'") {
        current += "'";
        index += 1;
        continue;
      }
      inString = !inString;
      continue;
    }

    if (char === "," && !inString) {
      values.push(normalizeSqlValue(current));
      current = "";
      continue;
    }

    current += char;
  }

  if (current.length > 0) {
    values.push(normalizeSqlValue(current));
  }

  return values;
}

function normalizeSqlValue(value) {
  const trimmed = String(value).trim();
  if (trimmed.toUpperCase() === "NULL") {
    return null;
  }
  return trimmed;
}

function buildUsers(config, rng) {
  const users = [];
  for (let index = 0; index < config.userCount; index += 1) {
    const id = config.startUserId + index;
    const surname = pick(rng, SURNAMES);
    const given = pick(rng, GIVEN_NAMES);
    const nickname = `${surname}${given}`;
    const hasWebPassword = rng() < 0.55;
    const hasOpenId = !hasWebPassword || rng() < 0.45;
    const phone = `139${String(config.startUserId + index).padStart(8, "0").slice(-8)}`;
    const createdAt = randomDateBetween(rng, daysAgo(320), daysAgo(90));
    const lastLoginAt = randomDateBetween(rng, createdAt, daysAgo(1));

    users.push({
      id,
      openid: hasOpenId ? `bulk_wx_openid_${id}` : null,
      nickname,
      phone,
      password: hasWebPassword ? USER_PASSWORD_HASH : null,
      avatarUrl: "/uploads/avatar/avatar.jpg",
      createdAt,
      updatedAt: lastLoginAt,
      lastLoginAt
    });
  }
  return users;
}

function buildDatasets(users, spots, config, rng) {
  const preferences = [];
  const favorites = [];
  const views = [];
  const reviews = [];
  const orders = [];

  let preferenceId = config.startPreferenceId;
  let favoriteId = config.startFavoriteId;
  let viewId = config.startViewId;
  let reviewId = config.startReviewId;
  let orderId = config.startOrderId;
  let orderNoSeq = config.startOrderNoSeq;

  const weightedSpots = buildWeightedSpots(spots);
  const pricedSpots = spots.filter((spot) => Number.isFinite(spot.price) && spot.price >= 0);

  for (const user of users) {
    const favoriteCount = randomInt(rng, 2, 6);
    const favoriteSpotIds = sampleDistinctWeightedIds(weightedSpots, favoriteCount, rng);
    const favoriteSpots = favoriteSpotIds.map((spotId) => getSpotById(spots, spotId)).filter(Boolean);

    for (const spot of favoriteSpots) {
      const createdAt = randomDateBetween(rng, daysAgo(120), daysAgo(10));
      favorites.push({
        id: favoriteId,
        userId: user.id,
        spotId: spot.id,
        createdAt,
        updatedAt: createdAt
      });
      favoriteId += 1;
    }

    const preferredCategoryIds = buildPreferenceCategoryIds(favoriteSpots, rng);
    for (const categoryId of preferredCategoryIds) {
      const createdAt = randomDateBetween(rng, user.createdAt, daysAgo(5));
      preferences.push({
        id: preferenceId,
        userId: user.id,
        tag: String(categoryId),
        createdAt,
        updatedAt: createdAt
      });
      preferenceId += 1;
    }

    const viewCount = randomInt(rng, 8, 18);
    const reviewCandidateIds = new Set(favoriteSpotIds);
    const orderCandidateIds = new Set(favoriteSpotIds);
    for (let index = 0; index < viewCount; index += 1) {
      const spot = pickWeightedSpot(weightedSpots, rng);
      if (!spot) {
        continue;
      }
      const createdAt = randomDateBetween(rng, daysAgo(60), daysAgo(1));
      const source = pickWeightedValue(
        rng,
        [
          { value: "home", weight: 28 },
          { value: "search", weight: 22 },
          { value: "recommendation", weight: 18 },
          { value: "guide", weight: 14 },
          { value: "detail", weight: 18 }
        ]
      ) || pick(rng, VIEW_SOURCES);
      const viewDuration = randomInt(rng, 45, 320);
      views.push({
        id: viewId,
        userId: user.id,
        spotId: spot.id,
        viewSource: source,
        viewDuration,
        createdAt
      });
      reviewCandidateIds.add(spot.id);
      if (rng() < 0.35) {
        orderCandidateIds.add(spot.id);
      }
      viewId += 1;
    }

    const reviewSpotIds = sampleDistinctFromSet(reviewCandidateIds, randomInt(rng, 1, 4), rng);
    for (const spotId of reviewSpotIds) {
      const spot = getSpotById(spots, spotId);
      if (!spot) {
        continue;
      }
      const createdAt = randomDateBetween(rng, daysAgo(90), daysAgo(1));
      reviews.push({
        id: reviewId,
        userId: user.id,
        spotId,
        score: pickWeightedValue(
          rng,
          [
            { value: 5, weight: 40 },
            { value: 4, weight: 36 },
            { value: 3, weight: 16 },
            { value: 2, weight: 6 },
            { value: 1, weight: 2 }
          ]
        ),
        comment: buildReviewComment(spot.name, rng),
        createdAt,
        updatedAt: createdAt
      });
      reviewId += 1;
    }

    const orderCount = randomInt(rng, 1, 3);
    const orderSpotIds = sampleDistinctFromSet(orderCandidateIds, orderCount, rng).filter(
      (spotId) => pricedSpots.some((spot) => spot.id === spotId)
    );
    for (const spotId of orderSpotIds) {
      const spot = getSpotById(spots, spotId);
      if (!spot) {
        continue;
      }
      const status = pickWeightedValue(
        rng,
        [
          { value: 4, weight: 40 },
          { value: 1, weight: 22 },
          { value: 0, weight: 18 },
          { value: 2, weight: 12 },
          { value: 3, weight: 8 }
        ]
      );
      const quantity = randomInt(rng, 1, 4);
      const createdAt = randomDateBetween(rng, daysAgo(100), daysAgo(3));
      const visitDate = formatSqlDate(randomDateBetween(rng, daysAgo(-15), daysAgo(-90)));
      const paidAt = status === 0 || status === 2 ? null : offsetDate(createdAt, randomInt(rng, 5, 120), "minutes");
      const cancelledAt = status === 2 ? offsetDate(createdAt, randomInt(rng, 30, 1440), "minutes") : null;
      const refundedAt = status === 3 && paidAt ? offsetDate(paidAt, randomInt(rng, 60, 2880), "minutes") : null;
      const completedAt = status === 4 && paidAt ? offsetDate(paidAt, randomInt(rng, 1440, 10080), "minutes") : null;
      orders.push({
        id: orderId,
        orderNo: `BULK${formatCompactDate(createdAt)}${String(orderNoSeq).padStart(5, "0")}`,
        userId: user.id,
        spotId,
        quantity,
        totalAmount: (Number(spot.price) || 0) * quantity,
        status,
        visitDate,
        contactName: user.nickname,
        contactPhone: user.phone,
        paidAt,
        cancelledAt,
        refundedAt,
        completedAt,
        createdAt,
        updatedAt: refundedAt || cancelledAt || completedAt || paidAt || createdAt
      });
      orderId += 1;
      orderNoSeq += 1;
    }
  }

  return { users, preferences, favorites, views, reviews, orders };
}

function buildPreferenceCategoryIds(favoriteSpots, rng) {
  const categories = Array.from(new Set(favoriteSpots.map((spot) => spot.categoryId).filter(Boolean)));
  if (!categories.length) {
    return sampleDistinctArray(CATEGORY_IDS, randomInt(rng, 1, 3), rng);
  }
  shuffleInPlace(categories, rng);
  return categories.slice(0, randomInt(rng, 1, Math.min(3, categories.length)));
}

function buildReviewComment(spotName, rng) {
  const templates = [
    `${spotName}整体体验比预期稳定，节奏安排合理会更舒服。`,
    `${spotName}适合预留完整时段，不建议压缩成匆忙打卡。`,
    `${spotName}现场氛围不错，避开高峰时段体验会更好。`,
    `${spotName}更适合慢慢逛，路线提前规划会省很多时间。`,
    `${spotName}适合作为当天核心行程，周边联动空间也比较大。`
  ];
  return pick(rng, templates);
}

function buildWeightedSpots(spots) {
  return spots.map((spot) => ({
    ...spot,
    weight: Math.max(Number(spot.heatScore) || 0, 1000)
  }));
}

function pickWeightedSpot(weightedSpots, rng) {
  return pickWeightedValue(
    rng,
    weightedSpots.map((spot) => ({ value: spot, weight: spot.weight }))
  );
}

function sampleDistinctWeightedIds(weightedSpots, count, rng) {
  const pool = weightedSpots.map((spot) => ({ ...spot }));
  const picked = [];
  while (pool.length && picked.length < count) {
    const spot = pickWeightedValue(
      rng,
      pool.map((item) => ({ value: item, weight: item.weight }))
    );
    if (!spot) {
      break;
    }
    picked.push(spot.id);
    const index = pool.findIndex((item) => item.id === spot.id);
    if (index >= 0) {
      pool.splice(index, 1);
    }
  }
  return picked;
}

function sampleDistinctFromSet(set, count, rng) {
  return sampleDistinctArray(Array.from(set), count, rng);
}

function sampleDistinctArray(array, count, rng) {
  const pool = [...array];
  shuffleInPlace(pool, rng);
  return pool.slice(0, Math.min(count, pool.length));
}

async function writeSqlFiles(outputDir, datasets) {
  const files = [
    {
      path: path.join(outputDir, "20_user.sql"),
      sql: buildUserSql(datasets.users)
    },
    {
      path: path.join(outputDir, "30_user_preference.sql"),
      sql: buildPreferenceSql(datasets.preferences)
    },
    {
      path: path.join(outputDir, "40_user_spot_favorite.sql"),
      sql: buildFavoriteSql(datasets.favorites)
    },
    {
      path: path.join(outputDir, "50_user_spot_view.sql"),
      sql: buildViewSql(datasets.views)
    },
    {
      path: path.join(outputDir, "60_user_spot_review.sql"),
      sql: buildReviewSql(datasets.reviews)
    },
    {
      path: path.join(outputDir, "70_order.sql"),
      sql: buildOrderSql(datasets.orders)
    }
  ];

  for (const file of files) {
    await fs.writeFile(file.path, file.sql, "utf8");
  }
}

function buildUserSql(users) {
  const rows = users.map((user) =>
    `  (${user.id}, ${sqlNullableString(user.openid)}, ${sqlString(user.nickname)}, ${sqlString(user.phone)}, ` +
    `${sqlNullableString(user.password)}, ${sqlString(user.avatarUrl)}, 0, ${sqlDatetime(user.lastLoginAt)}, ` +
    `${sqlDatetime(user.createdAt)}, ${sqlDatetime(user.updatedAt)})`
  );
  return joinInsertSql(
    "Append-only bulk user seed. Do not rerun data.sql on an existing customized database.",
    "INSERT INTO `user`",
    "  (`id`, `openid`, `nickname`, `phone`, `password`, `avatar_url`, `is_deleted`, `last_login_at`, `created_at`, `updated_at`)",
    rows
  );
}

function buildPreferenceSql(preferences) {
  const rows = preferences.map((item) =>
    `  (${item.id}, ${item.userId}, ${sqlString(item.tag)}, 0, ${sqlDatetime(item.createdAt)}, ${sqlDatetime(item.updatedAt)})`
  );
  return joinInsertSql(
    "Append-only bulk preference seed.",
    "INSERT INTO `user_preference`",
    "  (`id`, `user_id`, `tag`, `is_deleted`, `created_at`, `updated_at`)",
    rows
  );
}

function buildFavoriteSql(favorites) {
  const rows = favorites.map((item) =>
    `  (${item.id}, ${item.userId}, ${item.spotId}, 0, ${sqlDatetime(item.createdAt)}, ${sqlDatetime(item.updatedAt)})`
  );
  return joinInsertSql(
    "Append-only bulk favorite seed.",
    "INSERT INTO `user_spot_favorite`",
    "  (`id`, `user_id`, `spot_id`, `is_deleted`, `created_at`, `updated_at`)",
    rows
  );
}

function buildViewSql(views) {
  const rows = views.map((item) =>
    `  (${item.id}, ${item.userId}, ${item.spotId}, ${sqlString(item.viewSource)}, ${item.viewDuration}, ${sqlDatetime(item.createdAt)})`
  );
  return joinInsertSql(
    "Append-only bulk view seed.",
    "INSERT INTO `user_spot_view`",
    "  (`id`, `user_id`, `spot_id`, `view_source`, `view_duration`, `created_at`)",
    rows
  );
}

function buildReviewSql(reviews) {
  const rows = reviews.map((item) =>
    `  (${item.id}, ${item.userId}, ${item.spotId}, ${item.score}, ${sqlString(item.comment)}, 0, ${sqlDatetime(item.createdAt)}, ${sqlDatetime(item.updatedAt)})`
  );
  return joinInsertSql(
    "Append-only bulk review seed.",
    "INSERT INTO `user_spot_review`",
    "  (`id`, `user_id`, `spot_id`, `score`, `comment`, `is_deleted`, `created_at`, `updated_at`)",
    rows
  );
}

function buildOrderSql(orders) {
  const rows = orders.map((item) =>
    `  (${item.id}, ${sqlString(item.orderNo)}, ${item.userId}, ${item.spotId}, ${item.quantity}, ${sqlNumber(item.totalAmount, 2)}, ` +
    `${item.status}, ${sqlString(item.visitDate)}, ${sqlString(item.contactName)}, ${sqlString(item.contactPhone)}, ` +
    `${sqlNullableDatetime(item.paidAt)}, ${sqlNullableDatetime(item.cancelledAt)}, ${sqlNullableDatetime(item.refundedAt)}, ` +
    `${sqlNullableDatetime(item.completedAt)}, 0, ${sqlDatetime(item.createdAt)}, ${sqlDatetime(item.updatedAt)})`
  );
  return joinInsertSql(
    "Append-only bulk order seed.",
    "INSERT INTO `order`",
    "  (`id`, `order_no`, `user_id`, `spot_id`, `quantity`, `total_amount`, `status`, `visit_date`,",
    rows,
    "   `contact_name`, `contact_phone`, `paid_at`, `cancelled_at`, `refunded_at`, `completed_at`, `is_deleted`, `created_at`, `updated_at`)"
  );
}

function joinInsertSql(headerComment, insertLine, columnsLine, rows, extraColumnsLine = null) {
  const now = formatSqlDatetime(new Date());
  const lines = [
    `-- Generated by travel-server/scripts/generate-bulk-behavior-sql.mjs at ${now}`,
    `-- ${headerComment}`,
    ""
  ];

  if (!rows.length) {
    lines.push("-- No rows generated.");
    return `${lines.join("\n")}\n`;
  }

  lines.push(insertLine, columnsLine);
  if (extraColumnsLine) {
    lines.push(extraColumnsLine);
  }
  lines.push("VALUES", `${rows.join(",\n")};`, "");
  return `${lines.join("\n")}\n`;
}

function getSpotById(spots, spotId) {
  return spots.find((spot) => spot.id === spotId) || null;
}

function createRng(seed) {
  let state = seed >>> 0;
  return function next() {
    state = (state + 0x6D2B79F5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function pick(rng, array) {
  return array[Math.floor(rng() * array.length)];
}

function pickWeightedValue(rng, items) {
  const totalWeight = items.reduce((sum, item) => sum + Math.max(item.weight, 0), 0);
  if (totalWeight <= 0) {
    return items[0]?.value ?? null;
  }
  let threshold = rng() * totalWeight;
  for (const item of items) {
    threshold -= Math.max(item.weight, 0);
    if (threshold <= 0) {
      return item.value;
    }
  }
  return items[items.length - 1]?.value ?? null;
}

function shuffleInPlace(array, rng) {
  for (let index = array.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(rng() * (index + 1));
    [array[index], array[swapIndex]] = [array[swapIndex], array[index]];
  }
}

function randomInt(rng, min, max) {
  return Math.floor(rng() * (max - min + 1)) + min;
}

function daysAgo(days) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date;
}

function randomDateBetween(rng, start, end) {
  const startTime = new Date(start).getTime();
  const endTime = new Date(end).getTime();
  const min = Math.min(startTime, endTime);
  const max = Math.max(startTime, endTime);
  const offset = Math.floor(rng() * (max - min + 1));
  return new Date(min + offset);
}

function offsetDate(base, amount, unit) {
  const date = new Date(base);
  if (unit === "minutes") {
    date.setMinutes(date.getMinutes() + amount);
  }
  return date;
}

function formatSqlDatetime(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join("-") + " " + [
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds())
  ].join(":");
}

function formatCompactDate(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join("");
}

function formatSqlDate(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join("-");
}

function sqlString(value) {
  const text = String(value ?? "").replace(/\\/g, "\\\\").replace(/'/g, "''");
  return `'${text}'`;
}

function sqlNullableString(value) {
  return value == null ? "NULL" : sqlString(value);
}

function sqlDatetime(value) {
  return sqlString(formatSqlDatetime(new Date(value)));
}

function sqlNullableDatetime(value) {
  return value == null ? "NULL" : sqlDatetime(value);
}

function sqlNumber(value, fixed = null) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "0";
  }
  return fixed == null ? String(number) : number.toFixed(fixed);
}

function parseInteger(value, defaultValue) {
  if (value == null) {
    return defaultValue;
  }
  const number = Number.parseInt(String(value), 10);
  if (!Number.isFinite(number)) {
    throw new Error(`不是合法整数: ${value}`);
  }
  return number;
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
