#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const SERVER_ROOT = path.resolve(__dirname, "..");

const REGION_OPTIONS = [
  { id: 11, name: "北京市" },
  { id: 12, name: "上海市" },
  { id: 13, name: "广州市" },
  { id: 14, name: "深圳市" },
  { id: 15, name: "杭州市" },
  { id: 16, name: "成都市" },
  { id: 17, name: "西安市" },
  { id: 18, name: "重庆市" },
  { id: 19, name: "南京市" },
  { id: 20, name: "苏州市" },
  { id: 21, name: "长沙市" },
  { id: 22, name: "厦门市" }
];

const CATEGORY_OPTIONS = [
  { id: 7, name: "湖泊" },
  { id: 8, name: "山岳" },
  { id: 9, name: "森林公园" },
  { id: 10, name: "古建筑/宫殿" },
  { id: 11, name: "博物馆" },
  { id: 12, name: "古镇古村" },
  { id: 13, name: "游乐园" },
  { id: 14, name: "水上乐园" },
  { id: 15, name: "地标建筑" },
  { id: 16, name: "特色街区" },
  { id: 17, name: "温泉度假" },
  { id: 18, name: "海滨沙滩" },
  { id: 19, name: "徒步登山" },
  { id: 20, name: "漂流/露营" }
];

const EXISTING_SPOT_NAMES = [
  "故宫博物院",
  "颐和园",
  "上海迪士尼乐园",
  "外滩",
  "西湖",
  "灵隐飞来峰景区",
  "成都大熊猫繁育研究基地",
  "都江堰景区",
  "秦始皇帝陵博物院",
  "洪崖洞民俗风貌区",
  "广州塔",
  "深圳欢乐谷",
  "南京夫子庙",
  "拙政园",
  "橘子洲景区",
  "鼓浪屿"
];

const DEFAULT_OUTPUT = path.join(
  SERVER_ROOT,
  "src",
  "main",
  "resources",
  "db",
  "seed",
  "bulk",
  "10_spot.sql"
);

const DEFAULTS = {
  count: 12,
  startId: 1001,
  imageCount: 3,
  bannerCount: 6,
  output: DEFAULT_OUTPUT,
  model: process.env.APP_AI_GENERATION_MODEL || "qwen3.5-plus",
  temperature: Number(process.env.APP_AI_GENERATION_TEMPERATURE || "0.2")
};

async function main() {
  await loadEnvFile(path.join(SERVER_ROOT, ".env"));

  const args = parseArgs(process.argv.slice(2));
  if (args.help) {
    printHelp();
    return;
  }

  const config = buildConfig(args);
  validateConfig(config);

  console.log(`[1/4] 生成 ${config.count} 条景点草稿...`);
  const draft = await generateSpotDraft(config);

  console.log("[2/4] 补齐 Wikipedia / Wikimedia 图片链接...");
  const spotsWithImages = [];
  for (const spot of draft.spots) {
    const enrichedSpot = await enrichSpotImages(spot, config.imageCount);
    spotsWithImages.push(enrichedSpot);
  }

  console.log("[3/4] 组装 SQL...");
  const sql = buildSql(spotsWithImages, config);

  await fs.mkdir(path.dirname(config.output), { recursive: true });
  await fs.writeFile(config.output, sql, "utf8");

  console.log(`[4/4] 完成: ${config.output}`);
}

function printHelp() {
  console.log(`用法:
  node travel-server/scripts/generate-ai-spot-sql.mjs [options]

选项:
  --count <n>         生成景点数量，默认 12
  --start-id <n>      景点起始 ID，默认 1001
  --image-count <n>   每个景点最多生成多少条图库图，默认 3
  --banner-count <n>  额外生成多少条 banner SQL，默认 6
  --model <name>      覆盖生成模型，默认取 APP_AI_GENERATION_MODEL
  --temperature <n>   覆盖温度参数，默认取 APP_AI_GENERATION_TEMPERATURE
  --output <path>     输出 SQL 文件路径
  --help              查看帮助

环境变量:
  APP_AI_GENERATION_BASE_URL
  APP_AI_GENERATION_API_KEY
  APP_AI_GENERATION_MODEL
  APP_AI_GENERATION_TEMPERATURE
  OPENAI_BASE_URL
  OPENAI_API_KEY
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
  const baseUrl = (
    process.env.APP_AI_GENERATION_BASE_URL ||
    process.env.OPENAI_BASE_URL ||
    "https://dashscope.aliyuncs.com/compatible-mode/v1"
  ).replace(/\/+$/, "");

  const apiKey =
    process.env.APP_AI_GENERATION_API_KEY ||
    process.env.OPENAI_API_KEY ||
    "";

  return {
    count: parseInteger(args.count, DEFAULTS.count),
    startId: parseInteger(args["start-id"], DEFAULTS.startId),
    imageCount: parseInteger(args["image-count"], DEFAULTS.imageCount),
    bannerCount: parseInteger(args["banner-count"], DEFAULTS.bannerCount),
    output: args.output ? path.resolve(process.cwd(), args.output) : DEFAULTS.output,
    model: args.model || DEFAULTS.model,
    temperature: parseFloatValue(args.temperature, DEFAULTS.temperature),
    baseUrl,
    apiKey
  };
}

function validateConfig(config) {
  if (!config.apiKey) {
    throw new Error("缺少 API Key。请配置 APP_AI_GENERATION_API_KEY 或 OPENAI_API_KEY。");
  }
  if (config.count < 1 || config.count > 50) {
    throw new Error("--count 必须在 1 到 50 之间。");
  }
  if (config.startId < 1) {
    throw new Error("--start-id 必须大于 0。");
  }
  if (config.imageCount < 1 || config.imageCount > 6) {
    throw new Error("--image-count 必须在 1 到 6 之间。");
  }
  if (config.bannerCount < 0 || config.bannerCount > config.count) {
    throw new Error("--banner-count 必须在 0 到 count 之间。");
  }
}

async function generateSpotDraft(config) {
  const regionText = REGION_OPTIONS.map((item) => `${item.id}:${item.name}`).join("、");
  const categoryText = CATEGORY_OPTIONS.map((item) => `${item.id}:${item.name}`).join("、");
  const existingSpotText = EXISTING_SPOT_NAMES.join("、");

  const prompt = `
请生成 ${config.count} 个中国真实存在的旅游景点测试数据，严格输出 JSON 对象，不要输出 Markdown。

要求：
1. 只输出一个 JSON 对象，格式为 {"spots":[...]}。
2. 每个景点都必须是真实存在的中国景点，不能虚构。
3. 每个景点字段固定为：
   - name: 景点名
   - description: 60 到 120 字的中文简介
   - price: 数字，门票价格；免费填 0
   - openTime: 中文营业时间
   - address: 中文详细地址
   - latitude: 小数纬度
   - longitude: 小数经度
   - categoryId: 只能从以下分类 ID 中选一个
   - regionId: 只能从以下地区 ID 中选一个
   - heatLevel: 1 到 3 的整数
   - heatScore: 6500 到 9800 的整数
   - avgRating: 4.2 到 4.9 的一位小数
   - reviewCount: 80 到 8000 的整数
   - wikiTitle: 适合在中文 Wikipedia 检索图片的词条标题，优先与景点名一致
4. 不要重复景点，不要输出解释文字。
5. 地区 ID 只能用：${regionText}
6. 分类 ID 只能用：${categoryText}
7. 不要使用以下仓库里已有景点：${existingSpotText}
8. 优先覆盖不同城市和不同景点类型。
`.trim();

  const payload = {
    model: config.model,
    temperature: config.temperature,
    response_format: { type: "json_object" },
    messages: [
      {
        role: "system",
        content: "你是旅游内容数据生成器。你只输出合法 JSON，不输出注释和解释。"
      },
      {
        role: "user",
        content: prompt
      }
    ]
  };

  const response = await fetchJson(`${config.baseUrl}/chat/completions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${config.apiKey}`
    },
    body: JSON.stringify(payload)
  });

  const content = response?.choices?.[0]?.message?.content;
  if (!content) {
    throw new Error("模型没有返回可解析内容。");
  }

  const parsed = parseJsonContent(content);
  const spots = Array.isArray(parsed?.spots) ? parsed.spots : null;
  if (!spots || !spots.length) {
    throw new Error("模型返回的 JSON 不包含 spots 数组。");
  }

  const uniqueNames = new Set();
  const normalized = spots.slice(0, config.count).map((spot, index) => {
    const normalizedSpot = normalizeSpotDraft(spot, index);
    if (uniqueNames.has(normalizedSpot.name)) {
      throw new Error(`模型返回了重复景点: ${normalizedSpot.name}`);
    }
    uniqueNames.add(normalizedSpot.name);
    return normalizedSpot;
  });

  if (normalized.length < config.count) {
    throw new Error(`模型返回数量不足，预期 ${config.count} 条，实际 ${normalized.length} 条。`);
  }

  return { spots: normalized };
}

function normalizeSpotDraft(spot, index) {
  const region = REGION_OPTIONS.find((item) => item.id === Number(spot.regionId));
  const category = CATEGORY_OPTIONS.find((item) => item.id === Number(spot.categoryId));
  if (!region) {
    throw new Error(`第 ${index + 1} 条景点 regionId 无效: ${spot.regionId}`);
  }
  if (!category) {
    throw new Error(`第 ${index + 1} 条景点 categoryId 无效: ${spot.categoryId}`);
  }

  return {
    name: requireText(spot.name, `第 ${index + 1} 条景点缺少 name`),
    description: requireText(spot.description, `第 ${index + 1} 条景点缺少 description`),
    price: Number(spot.price ?? 0),
    openTime: requireText(spot.openTime, `第 ${index + 1} 条景点缺少 openTime`),
    address: requireText(spot.address, `第 ${index + 1} 条景点缺少 address`),
    latitude: Number(spot.latitude),
    longitude: Number(spot.longitude),
    categoryId: category.id,
    regionId: region.id,
    heatLevel: clampInteger(spot.heatLevel, 1, 3),
    heatScore: clampInteger(spot.heatScore, 6500, 9800),
    avgRating: clampFloat(spot.avgRating, 4.2, 4.9, 1),
    reviewCount: clampInteger(spot.reviewCount, 80, 8000),
    wikiTitle: requireText(
      spot.wikiTitle || spot.name,
      `第 ${index + 1} 条景点缺少 wikiTitle`
    )
  };
}

async function enrichSpotImages(spot, imageCount) {
  const matchedTitle = await resolveWikipediaTitle(spot.wikiTitle || spot.name);
  const imageBundle = matchedTitle
    ? await fetchWikipediaImages(matchedTitle, imageCount)
    : null;

  const coverImageUrl = imageBundle?.coverImageUrl || "";
  const galleryImages = imageBundle?.galleryImages?.length
    ? imageBundle.galleryImages
    : (coverImageUrl ? [coverImageUrl] : []);

  return {
    ...spot,
    wikiResolvedTitle: matchedTitle || spot.wikiTitle || spot.name,
    coverImageUrl,
    galleryImages: galleryImages.slice(0, imageCount)
  };
}

async function resolveWikipediaTitle(query) {
  const terms = Array.from(
    new Set([query, query.replace(/[（）()]/g, " ").trim()].filter(Boolean))
  );

  for (const term of terms) {
    const searchUrl =
      `https://zh.wikipedia.org/w/api.php?action=opensearch&limit=5&namespace=0&format=json&origin=*` +
      `&search=${encodeURIComponent(term)}`;
    const searchResult = await fetchJson(searchUrl, { method: "GET" }, true);
    const titles = Array.isArray(searchResult?.[1]) ? searchResult[1] : [];
    if (titles.length) {
      return titles[0];
    }
  }
  return null;
}

async function fetchWikipediaImages(title, imageCount) {
  const summaryUrl = `https://zh.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(title)}`;
  const summary = await fetchJson(summaryUrl, { method: "GET" }, true);
  const coverImageUrl =
    summary?.originalimage?.source ||
    summary?.thumbnail?.source ||
    "";
  const deduped = Array.from(new Set([coverImageUrl].filter(Boolean)));
  return {
    coverImageUrl: coverImageUrl || deduped[0] || "",
    galleryImages: deduped.slice(0, imageCount)
  };
}

function buildSql(spots, config) {
  const now = formatSqlDatetime(new Date());
  const spotRows = [];
  const imageRows = [];
  const bannerRows = [];

  let spotId = config.startId;
  let imageId = config.startId * 10;
  let bannerId = config.startId * 10;

  for (const spot of spots) {
    const currentSpotId = spotId;
    spotRows.push(
      `  (${currentSpotId}, ${sqlString(spot.name)}, ${sqlString(spot.description)}, ${sqlNumber(spot.price, 2)}, ` +
      `${sqlString(spot.openTime)}, ${sqlString(spot.address)}, ${sqlNumber(spot.latitude, 7)}, ` +
      `${sqlNumber(spot.longitude, 7)}, ${sqlString(spot.coverImageUrl)}, ${spot.categoryId}, ${spot.regionId}, ` +
      `${spot.heatLevel}, ${spot.heatScore}, ${sqlNumber(spot.avgRating, 1)}, ${spot.reviewCount}, 1, 0, ` +
      `${sqlString(now)}, ${sqlString(now)})`
    );

    for (let i = 0; i < spot.galleryImages.length; i += 1) {
      imageRows.push(
        `  (${imageId}, ${currentSpotId}, ${sqlString(spot.galleryImages[i])}, ${i + 1}, 0, ${sqlString(now)}, ${sqlString(now)})`
      );
      imageId += 1;
    }

    spotId += 1;
  }

  for (let i = 0; i < Math.min(config.bannerCount, spots.length); i += 1) {
    const currentSpotId = config.startId + i;
    const spot = spots[i];
    bannerRows.push(
      `  (${bannerId}, ${sqlString(spot.coverImageUrl)}, ${currentSpotId}, ${i + 1}, 1, 0, ${sqlString(now)}, ${sqlString(now)})`
    );
    bannerId += 1;
  }

  const lines = [
    "-- AI generated by travel-server/scripts/generate-ai-spot-sql.mjs",
    `-- Generated at ${now}`,
    "-- Append-only bulk spot seed. Do not use this file to reset existing base data.",
    "-- Requires existing region/category seed data from db/data.sql",
    "",
    "INSERT INTO `spot`",
    "  (`id`, `name`, `description`, `price`, `open_time`, `address`, `latitude`, `longitude`, `cover_image_url`,",
    "   `category_id`, `region_id`, `heat_level`, `heat_score`, `avg_rating`, `review_count`,",
    "   `is_published`, `is_deleted`, `created_at`, `updated_at`)",
    "VALUES",
    `${spotRows.join(",\n")};`,
    ""
  ];

  if (imageRows.length) {
    lines.push(
      "INSERT INTO `spot_image`",
      "  (`id`, `spot_id`, `image_url`, `sort_order`, `is_deleted`, `created_at`, `updated_at`)",
      "VALUES",
      `${imageRows.join(",\n")};`,
      ""
    );
  }

  if (bannerRows.length) {
    lines.push(
      "INSERT INTO `spot_banner`",
      "  (`id`, `image_url`, `spot_id`, `sort_order`, `is_enabled`, `is_deleted`, `created_at`, `updated_at`)",
      "VALUES",
      `${bannerRows.join(",\n")};`,
      ""
    );
  }

  return `${lines.join("\n")}\n`;
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

function sqlString(value) {
  const text = String(value ?? "").replace(/\\/g, "\\\\").replace(/'/g, "''");
  return `'${text}'`;
}

function sqlNumber(value, fixed) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "0";
  }
  return fixed == null ? String(number) : number.toFixed(fixed);
}

async function loadEnvFile(filePath) {
  try {
    const content = await fs.readFile(filePath, "utf8");
    for (const rawLine of content.split(/\r?\n/)) {
      const line = rawLine.trim();
      if (!line || line.startsWith("#")) {
        continue;
      }
      const separatorIndex = line.indexOf("=");
      if (separatorIndex < 0) {
        continue;
      }
      const key = line.slice(0, separatorIndex).trim();
      const value = line.slice(separatorIndex + 1).trim();
      if (key && process.env[key] == null) {
        process.env[key] = stripQuotes(value);
      }
    }
  } catch (error) {
    if (error.code !== "ENOENT") {
      throw error;
    }
  }
}

function stripQuotes(value) {
  if (
    (value.startsWith('"') && value.endsWith('"')) ||
    (value.startsWith("'") && value.endsWith("'"))
  ) {
    return value.slice(1, -1);
  }
  return value;
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

function parseFloatValue(value, defaultValue) {
  if (value == null) {
    return defaultValue;
  }
  const number = Number.parseFloat(String(value));
  if (!Number.isFinite(number)) {
    throw new Error(`不是合法数字: ${value}`);
  }
  return number;
}

function requireText(value, message) {
  const text = String(value ?? "").trim();
  if (!text) {
    throw new Error(message);
  }
  return text;
}

function clampInteger(value, min, max) {
  const number = Math.round(Number(value));
  if (!Number.isFinite(number)) {
    return min;
  }
  return Math.max(min, Math.min(max, number));
}

function clampFloat(value, min, max, fixed) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return min;
  }
  const bounded = Math.max(min, Math.min(max, number));
  return Number(bounded.toFixed(fixed));
}

function parseJsonContent(content) {
  const trimmed = String(content).trim();
  const withoutFence = trimmed
    .replace(/^```json\s*/i, "")
    .replace(/^```\s*/i, "")
    .replace(/\s*```$/i, "")
    .trim();
  return JSON.parse(withoutFence);
}

async function fetchJson(url, options, allow404 = false) {
  const response = await fetch(url, options);
  if (!response.ok) {
    if (allow404 && response.status === 404) {
      return null;
    }
    const body = await response.text();
    throw new Error(`请求失败 ${response.status} ${response.statusText}: ${url}\n${body}`);
  }
  const text = await response.text();
  if (!text) {
    return null;
  }
  return JSON.parse(text);
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
