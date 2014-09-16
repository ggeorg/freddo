/*
 * Copyright 2013-2014 ArkaSoft LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.arkasoft.freddo.messagebus;

import java.util.Iterator;
import java.util.NoSuchElementException;

import freddo.dtalk.util.LOG;

/**
 * Class for listener lists.
 * 
 * @param <T>
 */
final class ListenerList<T> implements Iterable<T> {
  private static final String TAG = LOG.tag(ListenerList.class);

  // Node containing a listener in the list
  private class Node {
    private Node previous;
    private Node next;
    private T listener;

    public Node(Node previous, Node next, T listener) {
      this.previous = previous;
      this.next = next;
      this.listener = listener;
    }
  }

  // Node iterator
  private class NodeIterator implements Iterator<T> {
    private Node node;

    public NodeIterator() {
      this.node = first;
    }

    public boolean hasNext() {
      return (node != null);
    }

    public T next() {
      if (node == null) {
        throw new NoSuchElementException();
      }

      T listener = node.listener;
      node = node.next;

      return listener;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  // First node in the list (we don't maintain a reference to the last
  // node, since we need to walk the list looking for duplicates on add)
  private Node first = null;

  /**
   * Adds a listener to the list, if it has not previously been added.
   * 
   * @param listener
   */
  public void add(T listener) {
    try {
      if (listener == null) {
        throw new IllegalArgumentException("listener is null.");
      }

      Node node = first;

      if (node == null) {
        first = new Node(null, null, listener);
      } else {
        while (node.next != null && node.listener != listener) {
          node = node.next;
        }

        if (node.next == null && node.listener != listener) {
          node.next = new Node(node, null, listener);
        } else {
          LOG.e(TAG, "Duplicate listener %s added to %s", listener, this);
          throw new RuntimeException();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Removes a listener from the list, if it has previously been added.
   * 
   * @param listener
   */
  public void remove(T listener) {
    if (listener == null) {
      throw new IllegalArgumentException("listener is null.");
    }

    Node node = first;
    while (node != null && node.listener != listener) {
      node = node.next;
    }

    if (node == null) {
      LOG.e(TAG, "Nonexistent listener %s removed from %s", listener, this);
    } else {
      if (node.previous == null) {
        first = node.next;

        if (first != null) {
          first.previous = null;
        }
      } else {
        node.previous.next = node.next;

        if (node.next != null) {
          node.next.previous = node.previous;
        }
      }
    }
  }

  /**
   * Tests the existence of a listener in the list.
   * 
   * @param listener
   * 
   * @return <tt>true</tt> if the listener exists in the list; <tt>false</tt>,
   *         otherwise.
   */
  public boolean contains(T listener) {
    if (listener == null) {
      throw new IllegalArgumentException("listener is null.");
    }

    Node node = first;
    while (node != null && node.listener != listener) {
      node = node.next;
    }

    return (node != null);
  }

  /**
   * Tests the emptiness of the list.
   * 
   * @return <tt>true</tt> if the list contains no listeners; <tt>false</tt>,
   *         otherwise.
   */
  public boolean isEmpty() {
    return (first == null);
  }

  /**
   * 
   */
  public Iterator<T> iterator() {
    return new NodeIterator();
  }
}