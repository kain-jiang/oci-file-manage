# UI/UX 设计稿

基于 **Material 3** 的页面原型(HTML,可直接浏览器打开预览)。

## 页面清单

| 页面 | 文件 | 对应功能 |
|------|------|----------|
| 首页 | `pages/首页.html` | shortcuts 卡片 + push/pull 入口 |
| 推送 | `pages/推送.html` | 文件选择 + 配置 + 进度 |
| 拉取 | `pages/拉取.html` | ref 输入 + 目标目录 + 进度 |
| 仓库 | `pages/仓库.html` | 查询 + artifact 列表 + 行操作 |
| 历史 | `pages/历史.html` | 活动记录 + 筛选 + 清空 |
| 设置 | `pages/设置.html` | 凭据 + shortcuts 管理 |
| 快捷仓库 | `pages/快捷仓库.html` | tag 列表 + 推送新版本 |

## 设计规范

- **主题**:Material 3,主色 indigo 600(`#4f46e5`),支持浅色/深色切换(`data-theme="light|dark"`)
- 设计 tokens 定义于各 HTML 的 `:root` 变量(`--oci-primary-*` 等),Compose 主题应映射到
  Material 3 `ColorScheme`(见 docs/01-architecture.md 技术选型)
- 字体、间距、圆角遵循 Material 3 默认规范

## 预览

直接双击 HTML 文件用浏览器打开即可(无外部依赖,全部内联)。
