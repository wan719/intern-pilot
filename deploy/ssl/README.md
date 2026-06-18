# HTTPS 证书目录

部署运行时需要在此目录放置以下文件：

- `internpilot.com.cn_bundle.crt`
- `internpilot.com.cn.key`

证书文件由 Docker Compose 以只读方式挂载到前端 Nginx 容器的 `/etc/nginx/ssl`。

严禁将真实证书、私钥或其他敏感凭据提交到 Git。仓库仅保留本说明文件和 `.gitkeep`。
