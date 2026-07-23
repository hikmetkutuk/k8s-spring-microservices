# Cluster Topolojisi

İki katman var: host makine üzerinde Docker'ın barındırdığı Kind node'ları, ve onun içinde çalışan gerçek Kubernetes cluster'ı.

## 1. Docker katmanı — Kind neyi neyin üzerine kuruyor

![Docker katmanı](images/docker-layer.png)

<details>
<summary>Mermaid kaynağı</summary>

```mermaid
flowchart TB
    subgraph HOST["macOS Host"]
        subgraph DOCKER["Docker Engine"]
            REG["kind-registry\ncontainer\nlocalhost:5001"]
            NODE["kind-control-plane\ncontainer\n(tek node, control-plane + worker)"]
            REG -. "containerd mirror\nlocalhost:5001 -> kind-registry:5000" .-> NODE
        end
    end

    style REG fill:#2d5f5f,stroke:#1a3d3d,color:#eee
    style NODE fill:#5a3d7a,stroke:#3d2854,color:#eee
```

</details>

Docker burada sadece **Kind'ın node'larını ve local registry'yi barındırıyor** — image build sürecinde (Jib) veya production'da yer almıyor.

## 2. Kubernetes katmanı — `kind-control-plane` container'ının içi

![Kubernetes katmanı](images/kubernetes-layer.png)

<details>
<summary>Mermaid kaynağı</summary>

```mermaid
flowchart TB
    subgraph CLUSTER["kind-control-plane node (Kubernetes)"]
        subgraph NS_MS["namespace: microservices"]
            GW["api-gateway\n:8080"]
            AUTH["auth-service\n:8081"]
            USER["user-service\n:8082"]
            CAT["catalog-service\n:8083"]
            TASK["task-service\n:8084"]
            NOTIF["notification-service\n:8085"]

            PG[("postgres\nStatefulSet")]
            REDIS[("redis")]
            KAFKA[("kafka\nKRaft")]

            GW --> AUTH
            GW --> USER
            GW --> CAT
            GW --> TASK
            GW --> NOTIF

            TASK -- "OpenFeign\n(circuit breaker+retry)" --> CAT
            TASK -- "produce:\ntask-created-events" --> KAFKA
            KAFKA -- "consume" --> NOTIF

            AUTH --> PG
            USER --> PG
            CAT --> PG
            TASK --> PG
            NOTIF --> PG
            CAT -. cache .-> REDIS
            GW -. rate limit .-> REDIS
        end

        subgraph NS_OBS["namespace: observability (release: obs)"]
            PROM["prometheus-server"]
            GRAF["grafana"]
            TEMPO["tempo"]
            LOKI["loki"]
            PTAIL["promtail\n(DaemonSet)"]

            GRAF --> PROM
            GRAF --> TEMPO
            GRAF --> LOKI
            PTAIL --> LOKI
        end

        GW -. "metrics\n/actuator/prometheus" .-> PROM
        AUTH -. metrics .-> PROM
        USER -. metrics .-> PROM
        CAT -. metrics .-> PROM
        TASK -. metrics .-> PROM
        NOTIF -. metrics .-> PROM

        GW -. "traces (OTLP)" .-> TEMPO
        AUTH -. traces .-> TEMPO
        CAT -. traces .-> TEMPO
        TASK -. traces .-> TEMPO
        NOTIF -. traces .-> TEMPO

        PTAIL -. "stdout logs\n(tüm pod'lar)" .-> NS_MS
    end

    style GW fill:#3d5a7a,stroke:#294061,color:#eee
    style PROM fill:#7a4a2d,stroke:#54331f,color:#eee
    style GRAF fill:#7a4a2d,stroke:#54331f,color:#eee
    style TEMPO fill:#7a4a2d,stroke:#54331f,color:#eee
    style LOKI fill:#7a4a2d,stroke:#54331f,color:#eee
    style PTAIL fill:#7a4a2d,stroke:#54331f,color:#eee
```

</details>

## Not

- **Düz çizgiler**: senkron/gerçek trafik akışı (HTTP, Kafka produce/consume, DB bağlantısı).
- **Noktalı çizgiler**: gözlemlenebilirlik sinyalleri (metrics scrape, trace export, log tail) — uygulama akışının parçası değil, yan kanal.
- `microservices` namespace'i içindeki servisler birbirine **cluster-internal DNS** ile ulaşıyor (`<service>.<namespace>.svc.cluster.local`, kısaltılmış `<service>` veya cross-namespace `<service>.<namespace>`).
- Cross-namespace görünen tek bağlantı: uygulama servisleri → `tempo.observability` (trace export), çünkü Tempo `observability` namespace'inde, uygulamalar `microservices`'te.
