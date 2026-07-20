rootProject.name = "k8s-spring-microservices"

include(
    "common",
    "auth-service",
    "user-service",
    "catalog-service",
    "task-service",
    "notification-service",
    "api-gateway"
)
