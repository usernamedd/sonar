"""
Solution search module (MOCK implementation).
Searches for solutions to a given problem and returns structured results.
"""
from __future__ import annotations

from typing import List

from .models import Solution


# Pre-defined solution sets keyed by topic for realistic mock results
_MOCK_RESULTS: dict = {
    "deploy": [
        Solution(
            title="使用 Ansible 实现自动化部署和配置管理",
            summary="Ansible 是一个开源的 IT 自动化工具，支持零停机滚动更新、配置管理和应用部署。其声明式的 Playbook 语法易于维护，适合多环境管理。",
            url="https://docs.ansible.com/ansible/latest/index.html",
        ),
        Solution(
            title="AWS CodePipeline + ECS 持续部署方案",
            summary="利用 AWS 原生的 CodePipeline 和 ECS (Fargate) 构建端到端 CI/CD 管道。支持蓝绿部署和金丝雀发布，与 CloudWatch 集成实现自动回滚。",
            url="https://aws.amazon.com/codepipeline/",
        ),
        Solution(
            title="Spinnaker 多云持续交付平台",
            summary="Spinnaker 是 Netflix 开源的持续交付平台，支持 AWS、GCP 和 Kubernetes 等多云环境。提供管道模板、自动触发和部署策略管理。",
            url="https://spinnaker.io/",
        ),
    ],
    "performance": [
        Solution(
            title="使用 Locust 实施全面的性能压测",
            summary="Locust 是一个 Python 编写的分布式负载测试工具，可以模拟百万级并发用户。支持自定义脚本和实时 Web UI 监控，帮团队量化系统容量。",
            url="https://locust.io/",
        ),
        Solution(
            title="gRPC 替代 REST 减少网络开销",
            summary="gRPC 基于 HTTP/2 和 Protocol Buffers，相比 JSON 格式的 REST API 可减少约 30-50% 的传输数据量。支持双向流式通信和负载均衡。",
            url="https://grpc.io/",
        ),
        Solution(
            title="接入 OpenTelemetry 实现全链路追踪",
            summary="OpenTelemetry 提供统一的分布式追踪标准，可追踪每个请求在微服务间的完整路径。结合 Jaeger 或 Grafana Tempo 可视化分析延迟瓶颈。",
            url="https://opentelemetry.io/",
        ),
    ],
    "database": [
        Solution(
            title="Vitess 实现水平分片和弹性扩展",
            summary="Vitess 是 YouTube 开源的数据库集群系统，为 MySQL 提供水平分片、连接池和自动故障转移。兼容原生 MySQL 协议，应用层几乎无需改动。",
            url="https://vitess.io/",
        ),
        Solution(
            title="TiDB 分布式数据库 HTAP 方案",
            summary="TiDB 是兼容 MySQL 协议的分布式数据库，支持在线水平扩展和实时 HTAP (混合事务/分析处理)。一行代码即可将 MySQL 应用迁移到 TiDB。",
            url="https://pingcap.com/tidb/",
        ),
        Solution(
            title="使用 gh-ost 实现无阻塞表结构变更",
            summary="gh-ost 是 GitHub 开源的在线 DDL 工具，通过 MySQL 二进制日志流实时同步，实现零阻塞的表结构变更。相比 pt-online-schema-change 更安全、可控。",
            url="https://github.com/github/gh-ost",
        ),
    ],
}

_FALLBACK_RESULTS = [
    Solution(
        title="Stack Overflow — 技术问答社区",
        summary="全球最大的技术问答社区，涵盖几乎所有编程语言和框架的问题解决方案。搜索类似问题通常能找到经过验证的答案。",
        url="https://stackoverflow.com/",
    ),
    Solution(
        title="GitHub Discussions — 项目讨论区",
        summary="许多开源项目使用 GitHub Discussions 作为社区交流平台，可以查看项目维护者和社区成员对常见问题的讨论。",
        url="https://github.com/features/discussions",
    ),
    Solution(
        title="Google 高级搜索技巧",
        summary="使用 site:、filetype:、intitle: 等高级搜索运算符可以更精准地定位技术文档和解决方案。",
        url="https://www.google.com/search?q=site:dev.to",
    ),
]


def _classify_problem(problem: str, background: str) -> str:
    """Classify the problem into a category for mock lookup."""
    combined = (problem + " " + background).lower()

    if any(kw in combined for kw in ["deploy", "release", "ci/cd", "cd", "delivery", "发布", "部署"]):
        return "deploy"
    if any(kw in combined for kw in ["perf", "slow", "latency", "response", "timeout", "cache", "性能", "慢", "延迟"]):
        return "performance"
    if any(kw in combined for kw in ["database", "mysql", "postgres", "migrate", "schema", "数据", "数据库"]):
        return "database"

    return "general"


def search(
    problem: str,
    background: str,
    provider: str = "mock",
    api_key: str = "mock-key",
) -> List[Solution]:
    """
    Search for solutions given a problem statement and background context.

    Args:
        problem: The core problem/question to search for.
        background: Additional background context.
        provider: Search provider name ("mock" for mock implementation).
        api_key: API key for the search provider.

    Returns:
        List of Solution objects (title, summary, url).
    """
    if provider != "mock":
        print(f"⚠️  非 mock 搜索提供者 '{provider}' 暂未实现，使用 mock 模式")

    print(f"🔎 正在搜索解决方案...")

    category = _classify_problem(problem, background)
    if category in _MOCK_RESULTS:
        results = _MOCK_RESULTS[category]
        print(f"   分类: {category}, 找到 {len(results)} 个相关方案")
    else:
        results = _FALLBACK_RESULTS
        print(f"   未找到特定分类, 返回 {len(results)} 个通用方案")

    return results
