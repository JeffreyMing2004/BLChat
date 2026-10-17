# BLChat 版本检测页面

`index.html` 是无第三方依赖的静态页面，支持仓库内全部 12 个构建模块对应的游戏版本，查询以下 5 个现有接口：

| 版本线 | HTTPS 接口路径 |
| --- | --- |
| 1.20.x | `/1.20/version.blchat` |
| 1.21.x | `/1.21/version.blchat` |
| 26.1.x | `/26/26.1/version.blchat` |
| 26.2.x | `/26/26.2/version.blchat` |
| 26.3.x | `/26/26.3/version.blchat` |

所有请求使用 `https://version.minguncle.cn`，15 秒超时，并禁用浏览器请求缓存。页面支持查询最新版、比较已安装版本、显示更新日志，以及失败后重试。版本比较按数字段进行，缺失段补零。所有 Minecraft 版本共用根目录 `version.properties` 中的完整版本号（当前 `1.5.308`），发布时需将以上 5 个接口同步更新；页面仍兼容旧的数字分段版本号。

## 部署

将 `index.html` 上传至 `version.minguncle.cn` 的站点根目录，配置 HTTPS，并继续提供上表中的数据文件。HTML 是供用户使用的查询界面，不能替代模组读取的 `version.blchat`。

推荐数据格式（版本和日志应填写实际已发布信息；以下仅为格式示例）：

```json
{
  "version": "1.5.308",
  "changelog": "修复说明第一行\n修复说明第二行"
}
```

也支持只包含版本号的纯文本。推荐使用 `Content-Type: application/json; charset=utf-8` 提供 JSON，发布后刷新 CDN 缓存。不要把不存在的 `.blchat` 路径重写到 HTML 首页，应返回 404。页面会拒绝 HTML、空内容及缺少有效版本号的响应，避免错误报告为最新版。

与接口同域部署无需 CORS 配置。若通过其他域名、localhost 或本地文件预览，版本服务器必须允许相应来源的跨域 GET 请求，否则浏览器会阻止查询。不要通过关闭浏览器安全设置规避此限制。

本次只提供页面源码，未部署服务器，也未生成或修改线上版本数据。
