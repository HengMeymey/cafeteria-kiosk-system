import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

// ===================== EXCEPTIONS =====================
class AuthenticationException extends Exception {
    public AuthenticationException(String msg) { super(msg); }
}

class OutOfStockException extends Exception {
    public OutOfStockException(String msg) { super(msg); }
}

class SlotFullException extends Exception {
    public SlotFullException(String msg) { super(msg); }
}

class PayrollLimitExceededException extends Exception {
    public PayrollLimitExceededException(String msg) { super(msg); }
}

class CancelDeadlineException extends Exception {
    public CancelDeadlineException(String msg) { super(msg); }
}

// ===================== DATA MODELS =====================
class MenuItem {
    int id;
    String name;
    double price;
    int stock;

    ReentrantLock lock = new ReentrantLock(); // Critical Section lock

    public MenuItem(int id, String name, double price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}

class DeliverySlot {
    String slot;
    int capacity;
    int reserved = 0;

    ReentrantLock lock = new ReentrantLock(); // Critical Section lock

    public DeliverySlot(String slot, int capacity) {
        this.slot = slot;
        this.capacity = capacity;
    }
}

class Employee {
    int id;
    String name;
    double payrollLimit;

    public Employee(int id, String name, double limit) {
        this.id = id;
        this.name = name;
        this.payrollLimit = limit;
    }
}

class Order {
    int id;
    Employee employee;
    MenuItem item;
    String slot;
    boolean inPreparation = false;

    public Order(int id, Employee e, MenuItem item, String slot) {
        this.id = id;
        this.employee = e;
        this.item = item;
        this.slot = slot;
    }
}

// ===================== SERVER =====================
class OrderingServer {
    Map<Integer, MenuItem> menu = new HashMap<>();
    Map<Integer, Employee> employees = new HashMap<>();
    Map<String, DeliverySlot> slots = new HashMap<>();
    List<Order> orders = new ArrayList<>();

    int orderCounter = 1;

    // AUTH
    public Employee authenticate(int empId) throws AuthenticationException {
        if (!employees.containsKey(empId)) {
            throw new AuthenticationException("Invalid Card!");
        }
        return employees.get(empId);
    }

    // PLACE ORDER
    public synchronized Order placeOrder(int empId, int itemId, String slotTime)
            throws Exception {

        Employee emp = authenticate(empId);
        MenuItem item = menu.get(itemId);
        DeliverySlot slot = slots.get(slotTime);

        if (item == null) throw new Exception("Item not found");

        // ===== Critical Section: Item Stock =====
        item.lock.lock();
        try {
            if (item.stock <= 0) {
                throw new OutOfStockException("Item out of stock");
            }
            item.stock--;
        } finally {
            item.lock.unlock();
        }

        // ===== Critical Section: Delivery Slot =====
        slot.lock.lock();
        try {
            if (slot.reserved >= slot.capacity) {
                throw new SlotFullException("Slot full");
            }
            slot.reserved++;
        } finally {
            slot.lock.unlock();
        }

        if (item.price > emp.payrollLimit) {
            throw new PayrollLimitExceededException("Payroll limit exceeded");
        }

        Order order = new Order(orderCounter++, emp, item, slotTime);
        orders.add(order);

        System.out.println("Order Placed: " + item.name + " for " + emp.name);
        return order;
    }

    // CHANGE ORDER
    public void changeOrder(Order order, MenuItem newItem) throws Exception {
        if (order.inPreparation) {
            throw new Exception("Cannot change, already in preparation");
        }
        order.item = newItem;
        System.out.println("Order changed to " + newItem.name);
    }

    // CANCEL ORDER
    public void cancelOrder(Order order) throws CancelDeadlineException {
        if (order.inPreparation) {
            throw new CancelDeadlineException("Too late to cancel");
        }
        orders.remove(order);
        System.out.println("Order canceled");
    }

    // QUERY MENU
    public void queryMenu() {
        System.out.println("Menu:");
        for (MenuItem item : menu.values()) {
            System.out.println(item.id + " - " + item.name + " ($" + item.price + ") stock=" + item.stock);
        }
    }

    // MANAGER FUNCTIONS
    public void createMenuItem(MenuItem item) {
        menu.put(item.id, item);
        System.out.println("Menu item created: " + item.name);
    }

    public void deleteMenuItem(int id) {
        menu.remove(id);
        System.out.println("Menu item deleted");
    }
}

// ===================== KIOSK =====================
class Kiosk {
    enum State { OFFLINE, IDLE, ACTIVE }

    State state = State.OFFLINE;
    OrderingServer server;

    public Kiosk(OrderingServer server) {
        this.server = server;
    }

    public void startUp() throws Exception {
        if (state != State.OFFLINE) throw new Exception("Already running");
        state = State.IDLE;
        System.out.println("Kiosk started");
    }

    public void shutDown() throws Exception {
        if (state != State.IDLE) throw new Exception("Cannot shutdown");
        state = State.OFFLINE;
        System.out.println("Kiosk shut down");
    }

    public void placeOrder(int empId, int itemId, String slot) {
        try {
            state = State.ACTIVE;
            Order order = server.placeOrder(empId, itemId, slot);

            // Simulate receipt printer
            System.out.println("[Receipt] Order #" + order.id + " confirmed");

            state = State.IDLE;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            state = State.IDLE;
        }
    }
}

// ===================== MAIN =====================
public class CafeteriaSystem {

    public static void main(String[] args) throws Exception {
        OrderingServer server = new OrderingServer();

        // Setup data
        server.createMenuItem(new MenuItem(1, "Burger", 10, 5));
        server.createMenuItem(new MenuItem(2, "Pizza", 12, 5));

        server.slots.put("12:00", new DeliverySlot("12:00", 2));

        server.employees.put(1, new Employee(1, "Alice", 50));
        server.employees.put(2, new Employee(2, "Bob", 5)); // low limit

        // Create kiosks
        Kiosk kiosk1 = new Kiosk(server);
        Kiosk kiosk2 = new Kiosk(server);

        kiosk1.startUp();
        kiosk2.startUp();

        // Query menu
        server.queryMenu();

        // Simulate concurrent orders
        Thread t1 = new Thread(() -> {
            kiosk1.placeOrder(1, 1, "12:00");
        });

        Thread t2 = new Thread(() -> {
            kiosk2.placeOrder(1, 1, "12:00");
        });

        Thread t3 = new Thread(() -> {
            kiosk2.placeOrder(2, 2, "12:00"); // payroll fail
        });

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        // Shutdown
        kiosk1.shutDown();
        kiosk2.shutDown();
    }
}