rootProject.name = "k8s-spring-microservices"

include(
    "libs:common",
    "services:auth-service",
    "services:user-service",
    "services:catalog-service",
    "services:task-service",
    "services:notification-service",
    "services:api-gateway"
)
