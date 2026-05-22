#!/usr/bin/env node

import fs from "node:fs/promises";
import crypto from "node:crypto";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const SERVER_ROOT = path.resolve(__dirname, "..");
const DB_ROOT = path.join(SERVER_ROOT, "src", "main", "resources", "db");
const DEFAULT_OUTPUT_DIR = path.join(DB_ROOT, "seed", "bulk");
const DEFAULT_UPLOAD_ROOT = path.join(SERVER_ROOT, "uploads");
const DEFAULT_SPOT_COVER_URL = "/uploads/spot/default/cover/default.jpg";
const DEFAULT_SPOT_GALLERY_URL = "/uploads/spot/default/gallery/default.jpg";

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
  DEFAULT_OUTPUT_DIR,
  "ai_spot.sql"
);

const DEFAULTS = {
  count: 12,
  startId: null,
  imageCount: 3,
  bannerCount: 0,
  output: DEFAULT_OUTPUT,
  uploadRoot: DEFAULT_UPLOAD_ROOT,
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
  const seedState = await loadSeedState(config);
  applySeedState(config, seedState);
  if (args["inspect-state"]) {
    printSeedState(config, seedState);
    return;
  }
  validateConfig(config);

  console.log(`[1/4] 生成 ${config.count} 条景点草稿，起始景点 ID ${config.startId}...`);
  const draft = await generateSpotDraft(config);

  console.log("[2/4] 补齐 Wikipedia / Wikimedia 图片并落盘到 uploads...");
  const spotsWithImages = [];
  for (const spot of draft.spots) {
    const enrichedSpot = await enrichSpotImages(spot, config);
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
  --start-id <n>      景点起始 ID，默认自动取现有最大 spot.id + 1
  --image-count <n>   每个景点最多生成多少条图库图，默认 3
  --banner-count <n>  额外生成多少条 banner SQL，默认 0
  --model <name>      覆盖生成模型，默认取 APP_AI_GENERATION_MODEL
  --temperature <n>   覆盖温度参数，默认取 APP_AI_GENERATION_TEMPERATURE
  --output <path>     输出 SQL 文件路径，默认 src/main/resources/db/seed/bulk/ai_spot.sql
  --upload-root <dir> 图片落盘根目录，默认 travel-server/uploads
  --inspect-state     只查看自动推断的 ID 和已有景点数量，不调用 AI，不写文件
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
    if (token === "--inspect-state") {
      args["inspect-state"] = true;
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
    startId: args["start-id"] == null ? DEFAULTS.startId : parseInteger(args["start-id"], DEFAULTS.startId),
    imageCount: parseInteger(args["image-count"], DEFAULTS.imageCount),
    bannerCount: parseInteger(args["banner-count"], DEFAULTS.bannerCount),
    output: args.output ? path.resolve(process.cwd(), args.output) : DEFAULTS.output,
    uploadRoot: args["upload-root"] ? path.resolve(process.cwd(), args["upload-root"]) : DEFAULTS.uploadRoot,
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
  if (!Number.isInteger(config.startId) || config.startId < 1) {
    throw new Error("--start-id 必须大于 0。");
  }
  if (!Number.isInteger(config.startImageId) || config.startImageId < 1) {
    throw new Error("自动推断的 spot_image 起始 ID 无效。");
  }
  if (!Number.isInteger(config.startBannerId) || config.startBannerId < 1) {
    throw new Error("自动推断的 spot_banner 起始 ID 无效。");
  }
  if (config.imageCount < 1 || config.imageCount > 6) {
    throw new Error("--image-count 必须在 1 到 6 之间。");
  }
  if (config.bannerCount < 0 || config.bannerCount > config.count) {
    throw new Error("--banner-count 必须在 0 到 count 之间。");
  }
}

async function loadSeedState(config) {
  const sqlFiles = await collectSqlFiles(DB_ROOT);
  const state = {
    maxSpotId: 0,
    maxSpotImageId: 0,
    maxSpotBannerId: 0,
    spotNames: new Set(EXISTING_SPOT_NAMES)
  };

  for (const filePath of sqlFiles) {
    const content = await fs.readFile(filePath, "utf8");
    scanSpotRows(content, state);
    scanIdRows(content, "spot_image", "maxSpotImageId", state);
    scanIdRows(content, "spot_banner", "maxSpotBannerId", state);
  }

  return state;
}

async function collectSqlFiles(rootDir) {
  const files = [];

  async function walk(currentDir) {
    let entries = [];
    try {
      entries = await fs.readdir(currentDir, { withFileTypes: true });
    } catch (error) {
      if (error.code === "ENOENT") {
        return;
      }
      throw error;
    }

    for (const entry of entries) {
      const entryPath = path.join(currentDir, entry.name);
      if (entry.isDirectory()) {
        await walk(entryPath);
        continue;
      }
      if (entry.isFile() && entry.name.endsWith(".sql")) {
        files.push(entryPath);
      }
    }
  }

  await walk(rootDir);
  return files;
}

function applySeedState(config, state) {
  config.startId = config.startId || state.maxSpotId + 1;
  config.startImageId = state.maxSpotImageId + 1;
  config.startBannerId = state.maxSpotBannerId + 1;
  config.existingSpotNames = state.spotNames;
  config.existingSpotNameSet = new Set(Array.from(state.spotNames).map(normalizeSpotName));
}

function printSeedState(config, state) {
  console.log("当前 seed 状态:");
  console.log(`  已有景点数量: ${state.spotNames.size}`);
  console.log(`  当前最大 spot.id: ${state.maxSpotId}`);
  console.log(`  下一个 spot.id: ${config.startId}`);
  console.log(`  当前最大 spot_image.id: ${state.maxSpotImageId}`);
  console.log(`  下一个 spot_image.id: ${config.startImageId}`);
  console.log(`  当前最大 spot_banner.id: ${state.maxSpotBannerId}`);
  console.log(`  下一个 spot_banner.id: ${config.startBannerId}`);
  console.log(`  默认输出文件: ${config.output}`);
  console.log(`  默认 banner 生成数量: ${config.bannerCount}`);
}

function scanSpotRows(sql, state) {
  for (const tuple of extractInsertTuples(sql, "spot")) {
    const fields = splitSqlFields(tuple);
    const id = Number.parseInt(fields[0], 10);
    if (Number.isInteger(id)) {
      state.maxSpotId = Math.max(state.maxSpotId, id);
    }

    const name = parseSqlString(fields[1]);
    if (name) {
      state.spotNames.add(name);
    }
  }
}

function scanIdRows(sql, tableName, stateKey, state) {
  for (const tuple of extractInsertTuples(sql, tableName)) {
    const fields = splitSqlFields(tuple);
    const id = Number.parseInt(fields[0], 10);
    if (Number.isInteger(id)) {
      state[stateKey] = Math.max(state[stateKey], id);
    }
  }
}

function extractInsertTuples(sql, tableName) {
  const tuples = [];
  const escapedTableName = tableName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const pattern = new RegExp(
    "INSERT\\s+INTO\\s+`?" + escapedTableName + "`?(?=\\s|\\()[\\s\\S]*?VALUES\\s*([\\s\\S]*?);",
    "gi"
  );

  let match;
  while ((match = pattern.exec(sql)) !== null) {
    tuples.push(...splitSqlTuples(match[1]));
  }
  return tuples;
}

function splitSqlTuples(source) {
  const tuples = [];
  let quote = false;
  let depth = 0;
  let start = -1;

  for (let i = 0; i < source.length; i += 1) {
    const char = source[i];
    const next = source[i + 1];

    if (char === "'" && quote && next === "'") {
      i += 1;
      continue;
    }
    if (char === "'") {
      quote = !quote;
      continue;
    }
    if (quote) {
      continue;
    }
    if (char === "(") {
      if (depth === 0) {
        start = i + 1;
      }
      depth += 1;
      continue;
    }
    if (char === ")") {
      depth -= 1;
      if (depth === 0 && start >= 0) {
        tuples.push(source.slice(start, i));
        start = -1;
      }
    }
  }

  return tuples;
}

function splitSqlFields(tuple) {
  const fields = [];
  let quote = false;
  let start = 0;

  for (let i = 0; i < tuple.length; i += 1) {
    const char = tuple[i];
    const next = tuple[i + 1];

    if (char === "'" && quote && next === "'") {
      i += 1;
      continue;
    }
    if (char === "'") {
      quote = !quote;
      continue;
    }
    if (!quote && char === ",") {
      fields.push(tuple.slice(start, i).trim());
      start = i + 1;
    }
  }

  fields.push(tuple.slice(start).trim());
  return fields;
}

function parseSqlString(value) {
  const text = String(value ?? "").trim();
  if (!text.startsWith("'") || !text.endsWith("'")) {
    return "";
  }
  return text.slice(1, -1).replace(/''/g, "'").replace(/\\\\/g, "\\").trim();
}

async function generateSpotDraft(config) {
  const regionText = REGION_OPTIONS.map((item) => `${item.id}:${item.name}`).join("、");
  const categoryText = CATEGORY_OPTIONS.map((item) => `${item.id}:${item.name}`).join("、");
  const existingSpotText = Array.from(config.existingSpotNames).sort().join("、");

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
    const normalizedSpot = {
      ...normalizeSpotDraft(spot, index),
      seedId: config.startId + index
    };
    const normalizedName = normalizeSpotName(normalizedSpot.name);
    if (uniqueNames.has(normalizedName)) {
      throw new Error(`模型返回了重复景点: ${normalizedSpot.name}`);
    }
    if (config.existingSpotNameSet.has(normalizedName)) {
      throw new Error(`模型返回了已存在景点: ${normalizedSpot.name}`);
    }
    uniqueNames.add(normalizedName);
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

async function enrichSpotImages(spot, config) {
  const matchedTitle = await resolveWikipediaTitle(spot.wikiTitle || spot.name);
  const imageBundle = matchedTitle
    ? await fetchWikipediaImages(matchedTitle, config.imageCount)
    : null;

  const remoteImages = imageBundle?.galleryImages?.length
    ? imageBundle.galleryImages
    : [];
  const localImages = await downloadSpotImages(spot, remoteImages, config);
  const coverImageUrl = localImages.coverImageUrl || DEFAULT_SPOT_COVER_URL;
  const galleryImages = localImages.galleryImages.length
    ? localImages.galleryImages
    : [DEFAULT_SPOT_GALLERY_URL];

  return {
    ...spot,
    wikiResolvedTitle: matchedTitle || spot.wikiTitle || spot.name,
    coverImageUrl,
    galleryImages: galleryImages.slice(0, config.imageCount)
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

async function downloadSpotImages(spot, remoteImages, config) {
  const slug = buildSpotSlug(spot);
  const coverDir = path.join(config.uploadRoot, "spot", slug, "cover");
  const galleryDir = path.join(config.uploadRoot, "spot", slug, "gallery");
  const localImages = [];

  for (let i = 0; i < remoteImages.length; i += 1) {
    const remoteUrl = remoteImages[i];
    try {
      const targetDir = i === 0 ? coverDir : galleryDir;
      const prefix = i === 0 ? `cover_${slug}` : `gallery_${slug}`;
      const stored = await downloadImage(remoteUrl, targetDir, prefix);
      localImages.push(`/uploads/spot/${slug}/${i === 0 ? "cover" : "gallery"}/${stored.filename}`);
    } catch (error) {
      console.warn(`图片下载失败，已使用默认图兜底: ${spot.name} ${remoteUrl} (${error.message})`);
    }
  }

  if (!localImages.length) {
    return {
      coverImageUrl: DEFAULT_SPOT_COVER_URL,
      galleryImages: [DEFAULT_SPOT_GALLERY_URL]
    };
  }

  return {
    coverImageUrl: localImages[0],
    galleryImages: localImages.length > 1 ? localImages.slice(1) : [localImages[0]]
  };
}

async function downloadImage(url, targetDir, filenamePrefix) {
  const response = await fetch(url, {
    headers: {
      "User-Agent": "WayTripSeedGenerator/1.0"
    }
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  const buffer = Buffer.from(await response.arrayBuffer());
  if (!buffer.length) {
    throw new Error("空图片内容");
  }

  const contentType = response.headers.get("content-type") || "";
  const extension = resolveImageExtension(url, contentType);
  const hash = crypto.createHash("sha1").update(buffer).digest("hex").slice(0, 6);
  const filename = `${filenamePrefix}_${formatDateCompact(new Date())}_${hash}${extension}`;
  await fs.mkdir(targetDir, { recursive: true });
  await fs.writeFile(path.join(targetDir, filename), buffer);
  return { filename };
}

function resolveImageExtension(url, contentType) {
  const normalizedType = contentType.toLowerCase();
  if (normalizedType.includes("image/png")) {
    return ".png";
  }
  if (normalizedType.includes("image/webp")) {
    return ".webp";
  }
  if (normalizedType.includes("image/jpeg") || normalizedType.includes("image/jpg")) {
    return ".jpg";
  }

  const ext = path.extname(new URL(url).pathname).toLowerCase();
  return [".jpg", ".jpeg", ".png", ".webp"].includes(ext) ? ext : ".jpg";
}

function buildSpotSlug(spot) {
  if (spot.seedId) {
    return `ai-spot-${spot.seedId}`;
  }

  const raw = String(spot.name || "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
  return raw || `ai-spot-${Date.now()}`;
}

function buildSql(spots, config) {
  const now = formatSqlDatetime(new Date());
  const spotRows = [];
  const imageRows = [];
  const bannerRows = [];

  let spotId = config.startId;
  let imageId = config.startImageId;
  let bannerId = config.startBannerId;

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

function formatDateCompact(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join("");
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

function normalizeSpotName(value) {
  return String(value ?? "")
    .trim()
    .replace(/\s+/g, "")
    .replace(/[（）]/g, (char) => (char === "（" ? "(" : ")"))
    .toLowerCase();
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
