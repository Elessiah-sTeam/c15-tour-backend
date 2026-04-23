---
name: new-endpoint
description: Scaffold a new REST endpoint following the project's layered architecture (Controller → Service → Repository → Entity → DTO → Mapper)
argument-hint: "<HTTP-method> /path description"
allowed-tools: Read, Write, Edit, Glob, Grep
---

Scaffold a new endpoint: **$ARGUMENTS**

Read an existing controller + service + mapper set (e.g. `TourController`, `TourService`, `TourMapper`) before writing anything to match the exact style.

Follow this structure:

**1. DTO(s)** — in `backend/src/main/java/com/c15tour/backend/dto/`
- `<Resource>Request.java` for request body (if POST/PUT)
- `<Resource>Response.java` for response body
- Use Java records if the existing code uses records, otherwise classes with Lombok `@Data`

**2. Entity** — in `entity/` only if a new table is needed
- Annotate with `@Entity`, `@Table`
- Use `@GeneratedValue(strategy = GenerationType.IDENTITY)` for IDs

**3. Flyway migration** — in `backend/src/main/resources/db/migration/`
- Name: `V<next_version>__<description>.sql` (check existing files for the current version number)
- Only if the entity requires a new table or column

**4. Repository** — in `repository/`, extend `JpaRepository<Entity, Long>`

**5. Mapper** — in `mapper/`, use MapStruct `@Mapper(componentModel = "spring")`

**6. Service** — in `service/`, annotate with `@Service`
- Inject repository via constructor injection (not `@Autowired` field injection)
- Throw appropriate exceptions for not-found cases

**7. Controller** — in `controller/`, annotate with `@RestController` and `@RequestMapping`
- Use `@PreAuthorize` or rely on `SecurityConfig` as appropriate
- Return `ResponseEntity<>` with correct HTTP status codes

After scaffolding, check `SecurityConfig.java` to confirm the new endpoint is correctly secured or explicitly public.
