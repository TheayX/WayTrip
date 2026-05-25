# travel-admin

WayTrip 管理后台，面向运营、内容维护与推荐系统回看场景，当前采用 `app / shared / modules` 三层结构组织代码。

## 技术栈

- Vue 3
- Vite 5
- Vue Router 4
- Pinia
- pinia-plugin-persistedstate
- Element Plus
- ECharts
- WangEditor
- Axios
- Sass
- ESLint 9

## 启动与构建

安装依赖：

```bash
npm install
```

本地开发：

```bash
npm run dev
```

生产构建：

```bash
npm run build
```

本地预览构建产物：

```bash
npm run preview
```

代码检查：

```bash
npm run lint
npm run lint:fix
```

默认开发地址：`http://localhost:3000`

## 环境变量

- 默认不创建 `.env.local` 也能直接开发
- 需要 HTTPS 反代、ngrok 或特殊代理联调时，再复制 `.env.example` 为 `.env.local`
- 当前可选变量：
  - `VITE_DEV_PROXY_TARGET`：本地开发代理目标，默认 `http://localhost:8080`
  - `VITE_API_ORIGIN`：接口源站地址，供资源地址拼接等场景读取
  - `VITE_DEV_HOST`：开发服务器绑定 Host
  - `VITE_DEV_ALLOWED_HOSTS`：额外允许访问的 Host 列表，多个值用英文逗号分隔

## 目录结构

```text
travel-admin/
├─ .env.example                     环境变量模板
├─ .env.local                       本机私有联调配置，由开发者自行创建，不提交 Git
├─ .gitignore                       Git 忽略规则
├─ eslint.config.mjs                ESLint Flat Config
├─ index.html                       Vite HTML 入口，定义页面挂载点与基础文档信息
├─ package.json                     依赖与 npm scripts 入口
├─ package-lock.json                npm 依赖锁文件
├─ vite.config.js                   Vite 配置，负责别名、代理、拆包与开发端口
├─ public/
│  └─ waytrip-standard-mark.svg     静态公共资源，构建时原样拷贝
└─ src/
  ├─ app/                          应用壳层，负责启动、布局、路由与全局状态注册
  │  ├─ App.vue                    根组件
  │  ├─ main.js                    应用启动入口，注册路由、Pinia、Element Plus 与全局样式
  │  ├─ layout/                    后台整体布局
  │  ├─ router/                    路由定义、登录守卫与标题设置
  │  └─ store/                     应用级状态，如登录态、主题状态
  ├─ shared/                       共享层，放跨模块复用的能力与基础资源
  │  ├─ api/
  │  │  └─ request.js              Axios 请求封装，统一鉴权、错误处理与基础配置
  │  ├─ assets/
  │  │  └─ brand/                  品牌资源，如 Logo 与标识图
  │  ├─ composables/               跨模块复用逻辑，如主题切换、表格排序、通知拉取
  │  ├─ constants/                 全局常量，如导航分组、主题常量、资源展示与来源映射
  │  ├─ lib/                       通用工具，如资源地址处理、展示名兜底、消息框封装
  │  ├─ styles/
  │  │  ├─ index.scss              全局样式总入口
  │  │  └─ theme/                  主题 token、覆盖样式与样式工具
  │  └─ theme/
  │     └─ charts/                 图表主题配置
  └─ modules/                      业务模块层，按领域拆分后台页面与接口
     ├─ banner/                    轮播图管理
     ├─ category/                  分类管理
     ├─ guide/                     攻略管理
     ├─ order/                     订单中心
     ├─ overview/                  运营概览首页
     ├─ recommendation/            推荐系统总览与配置
     ├─ region/                    地区管理
     ├─ spot/                      景点管理
     ├─ system/                    系统模块，当前包含登录与管理员管理
     └─ user-ops/                  用户运营，包含用户、评价、收藏、偏好、浏览行为
```

## 模块组织方式

模块内部按复杂度按需拆分，不强制所有目录都出现：

```text
light-module/
├─ api.js
└─ index.vue

feature-module/
├─ api.js 或 api/
├─ components/
├─ composables/                     可选，仅在模块内存在复用逻辑时创建
├─ pages/                           可选，存在多页面或页面级拆分时使用
└─ styles/                          可选，存在模块级样式文件时使用
```

当前项目中的典型例子：

- `overview / banner / category / region` 属于轻量模块，通常以 `api.js + index.vue` 为主
- `spot / guide / order / recommendation / user-ops / system` 属于复杂模块，按页面、组件、接口进一步拆分

## 当前能力

- 管理员登录与路由鉴权
- 运营概览首页
- 景点、攻略、轮播图、地区、分类维护
- 订单中心
- 用户管理、评价管理、收藏记录、用户偏好、浏览行为回看
- 管理员管理
- 推荐总览与推荐配置
- AI 知识文档维护、RAG 检索 preview、命中结果查看与联调工作台

## AI 客服职责

- 管理端负责 AI 知识文档维护、检索 preview、命中知识域查看和联调验证。
- 当前 AI preview 已支持展示多知识域命中结果，便于排查平台规则、账号帮助和订单边界等召回是否符合预期。
- 管理端不承接真实对话编排，实际 AI 生成、记忆、SSE 输出和工具调用链路由 `travel-server` 统一处理。

## 开发约定

- 后台代码按 `app / shared / modules` 三层划分职责，不回退到旧的全局平铺结构
- 业务模块内部按需拆分 `pages / components / api / composables / styles`
- 共享能力优先放入 `shared`，避免业务模块之间相互引用页面级代码
- 新增注释统一使用中文，只补关键原因，不写翻译代码式注释
- 模块结构优先服务维护成本，不为了“看起来规范”创建空目录

## 文档后续可补充

- 页面路由清单：补充每个菜单项对应的路径、模块与用途，方便新成员定位入口
- 接口映射表：补充各模块 `api.js` 或 `api/` 与后端接口域的对应关系
- 状态与持久化说明：补充 `user`、`theme` 两个 store 的职责与持久化边界
- 联调说明：补充常见 `.env.local` 示例，以及反代、ngrok、资源预览的配置方式

## 相关文档

- [仓库总览](../README.md)
- [设计文档](../docs/specs/travel-recommendation-system/design.md)
- [API 文档](../docs/specs/travel-recommendation-system/api.md)
