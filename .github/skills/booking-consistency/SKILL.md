---
name: booking-consistency
description: Ensure consistency and avoid overbooking in reservation systems
---

# Rules
- Always validate availability before booking
- Use transactions for booking operations
- Prevent race conditions (optimistic or pessimistic locking)
- Avoid double booking of the same resource and time slot

# Patterns
- Use @Transactional in service layer
- Use versioning (@Version) for optimistic locking
- Re-check availability before final commit