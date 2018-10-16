package com.bittiger.logic;

import java.util.HashMap;
import java.util.Map;

public class LRUCache {
	class DLLinkedList {        
        int value;
        String key;
        DLLinkedList pre;
        DLLinkedList next;
        
        public DLLinkedList(){
            
        }
        
        public DLLinkedList(String inputKey, int inputValue){
            key = inputKey;
            value = inputValue;
        }
    }
    
    DLLinkedList head;
    DLLinkedList tail;
    Map<String, DLLinkedList> cache;
    int cacheCapacity;
    int elementCount;
    
    public void removeNode(DLLinkedList node){
        DLLinkedList preNode = node.pre;
        DLLinkedList nextNode = node.next;
        preNode.next = nextNode;
        nextNode.pre = preNode;
    }
    
    // Always add new node right after the head.
    public DLLinkedList addNode(DLLinkedList node){
        node.next = head.next;
        node.pre = head;
        
        head.next.pre = node;
        head.next = node;
        return node;
    }
    
    // Always pop node right before the tail.
    public DLLinkedList popNodeAtTail(){
        DLLinkedList node = tail.pre;
        tail.pre = tail.pre.pre;
        tail.pre.next = tail;
        return node;
    }
    
    public LRUCache(int capacity) {
        head = new DLLinkedList();
        head.pre = null;
        
        tail = new DLLinkedList();
        tail.next = null;
        
        head.next = tail;
        tail.pre = head;
        
        cache = new HashMap<String, DLLinkedList>();
        cacheCapacity = capacity;
        elementCount = 0;        
    }
    
    public int get(String key) {
        DLLinkedList node = new DLLinkedList();
        if (cache.containsKey(key)){
            node = cache.get(key);
            removeNode(node);
            addNode(node);
        }
        else
            node.value = -1;
        return node.value;
    }
    
    public void put(String key, int value) {
        DLLinkedList node = new DLLinkedList(key, value);
        
        if (cache.containsKey(key)){
            node = cache.get(key);
            node.value = value;
            removeNode(node);
            node = addNode(node);
            cache.put(key, node);
        }
        else{
            if (elementCount < cacheCapacity){
                elementCount++;
                node = addNode(node);
                cache.put(key, node);
            }
            else{
                DLLinkedList tmpNode = popNodeAtTail();
                cache.remove(tmpNode.key);
                node = addNode(node);                
                cache.put(key, node);
            }                
        }                        
    }
}
