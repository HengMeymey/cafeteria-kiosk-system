# ws0_6조_카페테리아_pwt.md
# Prompt Used for AI Code Generation – Cafeteria Ordering System

---

## Context

The system modeled is the **Cafeteria Ordering Kiosk System** described in the
Problem Description provided by the professor.

---

## Prompt

```
You are a senior Java software engineer.

Generate a complete, single-file Java program for a Cafeteria Ordering Kiosk System
based on the following requirements. Do not use any external libraries beyond the
Java standard library.

### System Overview
Process Impact Corporation operates several cafeteria ordering kiosks geographically
distributed across campus buildings, connected via a wide area network to a central
Ordering Server.

### Hardware per Kiosk
- Card Reader: reads employee card for authentication and payment
- Keyboard/Display: user input and output
- Receipt Printer: prints order confirmation and receipts

### Actors
1. Employee – places, changes, cancels orders, and queries the menu via the kiosk
2. Cafeteria Manager – creates, modifies, and deletes menu items
3. Kiosk Operator – starts up and shuts down the kiosk terminal
4. Card Reader (device actor) – supplies employee card data to the system

### Use Cases to Implement
| Use Case              | Normal Condition                                              |
|-----------------------|---------------------------------------------------------------|
| Place Order           | Auth success, stock available, delivery slot free, within payroll limit |
| Change Order          | Auth success, order not yet in preparation                    |
| Cancel Order          | Auth success, before preparation deadline                     |
| Query Menu            | Auth success, menu data exists                                |
| Create/Modify/Delete Menu | Cafeteria Manager authenticated                           |
| Start Up Kiosk        | Kiosk Operator authenticated, kiosk currently OFFLINE         |
| Shut Down Kiosk       | Kiosk Operator authenticated, kiosk currently IDLE            |

### Alternative / Error Flows
- Menu item out of stock → reject order, suggest alternative
- Delivery time slot full → reject order, suggest another slot
- Payroll deduction limit exceeded → reject payment, suggest card payment
- Cancel attempted after preparation deadline → reject with message
- Card authentication failure → retry up to 3 times, then terminate session
- User cancels transaction before payment confirmation → return to IDLE

### Concurrency Requirements (Critical)
Multiple kiosks may simultaneously:
- Access the same menu item stock → use per-item ReentrantLock (Critical Section)
- Reserve the same delivery time slot → use per-slot ReentrantLock (Critical Section)
- Simulate multiple kiosks using Java threads in main() to demonstrate concurrent access to stock and delivery slots.

### Data Management (Ordering Server)
All data is stored and managed centrally on the Ordering Server:
- Employee records
- Order records
- Menu and stock records
- Payroll deduction records
- Delivery records

### Implementation Requirements
- Use Java standard library only (java.util.concurrent for threading/locks)
- Model each actor as a class with appropriate methods
- Model each use case as a method on the relevant class
- Include proper exception classes for each error flow
- Include a main() method that demonstrates all use cases
- Add comments explaining Critical Sections
- Simulate the WAN connection between Kiosk and OrderingServer via method calls
- Simulate the card reader, display, and receipt printer with System.out output
- Log key system events (order placed, rejected, canceled) to the console for traceability.
```

---

## Notes on Prompt Design

| Principle | How Applied |
|---|---|
| **Role assignment** | "You are a senior Java software engineer" anchors the model's output style |
| **Structured requirements** | Tables for Use Cases and Alternative Flows give the model clear, parseable specs |
| **Explicit constraints** | "Java standard library only", "single file" prevent unwanted complexity |
| **Concurrency specification** | Explicitly named ReentrantLock and Critical Section to elicit correct concurrent design |
| **Demonstration requirement** | `main()` with all use cases forces the model to verify its own code compiles and runs |
| **Actor-as-class mapping** | Directly maps COMET actors to Java classes to test how well AI follows UML-driven design |
