# Modernization Plan: CampusConnect Azure Modernization

**Project**: CampusConnect

## Technical Framework

- **Language**: Java 25
- **Framework**: Spring Boot 4.1.2, Spring Security
- **Build Tool**: Maven Wrapper 3.9.16
- **Database**: MongoDB 9
- **Key Dependencies**: Spring Data MongoDB, Spring Web MVC, Spring Security,
  Spring Security JOSE

## Overview

> This modernization prepares CampusConnect for Azure deployment while moving
> its database connectivity away from a local MongoDB instance. The application
> currently uses Spring Boot and local-development endpoint defaults. The
> modernized application will:
>
> - Connect to Azure DocumentDB with MongoDB compatibility using Microsoft
>   Entra ID authentication.
> - Keep local-only URLs and HTTP behavior isolated to development and test
>   environments, and use configurable secure endpoints in production.
> - Deploy the Spring application to Azure Container Apps, using an existing
>   or externally provided environment rather than provisioning new resources.
>
> The work proceeds through database and endpoint changes, security validation,
> integration verification, and deployment.

## Migration Impact Summary

| Application | Existing -> target | Authentication |
|---|---|---|
| CampusConnect | Local MongoDB -> Azure DocumentDB | Microsoft Entra ID |
| CampusConnect | URLs -> secure endpoints | HTTPS in production |
| CampusConnect | Spring Boot -> Azure Container Apps | Existing environment |

## Assessment Validation

The report's MongoDB configuration, server-port, localhost, local HTTP,
hardcoded-URL, and secure-protocol findings were confirmed in the current
workspace. The password-property finding was not treated as a plaintext
credential: the current configuration references environment variables. The
reported default-password location is currently a regular
`User.getPassword()` accessor, so that finding is excluded. No Java version
upgrade was requested.

## Open Questions & Questionnaire

- [x] Q: Should the plan provision new infrastructure? → A: No new provisioning;
  use an existing or externally provided environment (inferred default because
  the user was unavailable and no Azure infrastructure configuration was found).
- [x] Q: Should integration tests be included? → A: Yes, mock mode (inferred
  default because the user was unavailable).
- [x] Q: Should the plan include security/CVE remediation? → A: Yes (required
  default for modernization plans).
- [x] Q: Which Azure deployment target should be used? → A: Azure Container Apps
  (inferred default because the user was unavailable).
- [x] Q: Should a separate containerization task be included? → A: No; Azure
  Container Apps deployment covers containerization.
