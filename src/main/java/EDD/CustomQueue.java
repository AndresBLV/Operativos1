/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EDD;

/**
 *
 * @author Indatech
 */
public class CustomQueue<T> {

    private class Node {

        T data;
        Node next;

        Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node head;
    private Node tail;
    private int size;

    public CustomQueue() {
        head = null;
        tail = null;
        size = 0;
    }

    public void enqueue(T item) {
        Node newNode = new Node(item);
        if (isEmpty()) {
            head = tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }

    public T dequeue() {
        if (isEmpty()) {
            return null;
        }

        T item = head.data;
        head = head.next;
        size--;

        if (isEmpty()) {
            tail = null;
        }

        return item;
    }

    public void remove(T item) {
        if (isEmpty()) {
            return;
        }

        if (head.data.equals(item)) {
            dequeue();
            return;
        }

        Node current = head;
        while (current.next != null && !current.next.data.equals(item)) {
            current = current.next;
        }

        if (current.next != null) {
            current.next = current.next.next;
            size--;
            if (current.next == null) {
                tail = current;
            }
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void printQueue() {
        Node current = head;
        System.out.print("Queue: ");
        while (current != null) {
            System.out.print(current.data + " -> ");
            current = current.next;
        }
        System.out.println("null");
    }
}
