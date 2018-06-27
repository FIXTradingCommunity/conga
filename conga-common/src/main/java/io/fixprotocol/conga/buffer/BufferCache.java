/*
 * Copyright 2018 FIX Protocol Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package io.fixprotocol.conga.buffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;


/**
 * A cache of recent message buffers
 * <p>
 * Not persistent. In this implementation, the size of the cache is preallocated and old messages
 * are overwritten by new ones.
 * 
 * @author Don Mendelson
 *
 */
public class BufferCache implements List<ByteBuffer> {

  private class BufferIterator implements ListIterator<ByteBuffer> {

    private int cursor;

    /**
     * @param index
     */
    public BufferIterator(int index) {
      cursor = index;
    }

    /**
     * Unsupported operation
     */
    @Override
    public void add(ByteBuffer e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNext() {
      return cursor < getMaximumAvailableIndex();
    }

    @Override
    public boolean hasPrevious() {
      return cursor > getMinimumAvailableIndex();
    }

    @Override
    public ByteBuffer next() {
      try {
        int i = cursor;
        ByteBuffer next = get(i);
        cursor = i + 1;
        return next.duplicate();
      } catch (IndexOutOfBoundsException e) {
        throw new NoSuchElementException();
      }
    }

    @Override
    public int nextIndex() {
      return cursor;
    }

    @Override
    public ByteBuffer previous() {
      try {
        int i = cursor - 1;
        ByteBuffer prev = get(i);
        cursor = i;
        return prev.duplicate();
      } catch (IndexOutOfBoundsException e) {
        throw new NoSuchElementException();
      }
    }

    @Override
    public int previousIndex() {
      return cursor - 1;
    }

    /**
     * Unsupported operation
     */
    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void set(ByteBuffer src) {
      BufferCache.this.set(cursor, src);
    }

  }

  public static final int DEFAULT_BUFFER_CAPACITY = 1024;

  public static final int DEFAULT_CACHE_CAPACITY = 16;

  private final ByteBuffer[] cache;
  private int maxIndex = -1;

  public BufferCache() {
    this(DEFAULT_CACHE_CAPACITY, DEFAULT_BUFFER_CAPACITY);
  }

  public BufferCache(int cacheCapacity, int bufferCapacity) {
    cache = new ByteBuffer[cacheCapacity];
    for (int i = 0; i < cache.length; i++) {
      cache[i] = ByteBuffer.allocateDirect(bufferCapacity);
    }
  }

  @Override
  public boolean add(ByteBuffer src) {
    if (null == src) {
      return false;
    }
    maxIndex++;
    int position = position(maxIndex);
    ByteBuffer element = getElement(position);
    copyBuffer(src, element);
    return true;
  }

  /**
   * Only supports appending new buffers at higher indexes
   * <p>
   * Warning: if intervening buffers are not populated by calling {@link #set(int, ByteBuffer)},
   * they will remain empty.
   * 
   * @param index sequence number of the added buffer; must be equal to or higher than
   *        {@link #size()} but not greater than size() + cache capacity
   * @param src buffer to append
   * @throws IndexOutOfBoundsException if index is out of range
   */
  @Override
  public void add(int index, ByteBuffer src) {
    if ((index < getMinimumAvailableIndex()) || (index > (getMinimumAvailableIndex() + cache.length))) {
      throw new IndexOutOfBoundsException();
    } else {
      int max = getMaximumAvailableIndex();
      // Clear intervening buffers
      for (int i = max; i < index; i++) {
        int position = position(i);
        ByteBuffer element = getElement(position);
        element.clear();
      }
      maxIndex = index;
      set(index, src);
    }
  }

  @Override
  public boolean addAll(Collection<? extends ByteBuffer> c) {
    boolean modified = false;
    for (ByteBuffer buffer : c) {
      add(buffer);
      modified = true;
    }
    return modified;
  }

  @Override
  public boolean addAll(int index, Collection<? extends ByteBuffer> c) {
    boolean modified = false;
    int i = index;
    for (ByteBuffer buffer : c) {
      add(i, buffer);
      i++;
      modified = true;
    }
    return modified;
  }

  @Override
  public void clear() {
    maxIndex = -1;
  }

  /**
   * Unsupported operation
   */
  @Override
  public boolean contains(Object o) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported operation
   */
  @Override
  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ByteBuffer get(int index) {
    if (index < getMinimumAvailableIndex() || index > getMaximumAvailableIndex()) {
      throw new IndexOutOfBoundsException();
    } else {
      int position = position(index);
      return cache[position].duplicate();
    }
  }

  @Override
  public int indexOf(Object o) {
    ListIterator<ByteBuffer> it = listIterator(getMinimumAvailableIndex());
    while (it.hasNext()) {
      if (o.equals(it.next()))
        return it.previousIndex();
    }
    return -1;
  }

  @Override
  public boolean isEmpty() {
    return getMaximumAvailableIndex() < 0;
  }

  @Override
  public Iterator<ByteBuffer> iterator() {
    return listIterator(0);
  }

  @Override
  public int lastIndexOf(Object o) {
    ListIterator<ByteBuffer> it = listIterator(getMinimumAvailableIndex());
    while (it.hasPrevious()) {
      if (o.equals(it.previous()))
        return it.nextIndex();
    }
    return -1;
  }

  @Override
  public ListIterator<ByteBuffer> listIterator() {
    return listIterator(0);
  }

  @Override
  public ListIterator<ByteBuffer> listIterator(int index) {
    return new BufferIterator(index);
  }

  /**
   * Unsupported operation
   */
  @Override
  public ByteBuffer remove(int index) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported operation
   */
  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported operation
   */
  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported operation
   */
  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ByteBuffer set(int index, ByteBuffer src) {
    if (index < getMinimumAvailableIndex() || index > getMaximumAvailableIndex()) {
      throw new IndexOutOfBoundsException();
    } else {
      int position = position(index);
      ByteBuffer element = getElement(position);
      copyBuffer(src, element);
      return element.duplicate();
    }
  }

  @Override
  public int size() {
    return getMaximumAvailableIndex() + 1;
  }

  @Override
  public List<ByteBuffer> subList(int fromIndex, int toIndex) {
    if (fromIndex < getMinimumAvailableIndex() || toIndex > getMaximumAvailableIndex()) {
      throw new IndexOutOfBoundsException();
    }
    List<ByteBuffer> array = new ArrayList<>(toIndex - fromIndex);
    ListIterator<java.nio.ByteBuffer> iter = listIterator(fromIndex);
    int j = 0;
    while (iter.hasNext()) {
      array.set(j, iter.next().duplicate());
      j++;
    }
    for (; j < array.size(); j++) {
      array.set(j, ByteBuffer.allocate(0));
    }
    return array;
  }

  @Override
  public Object[] toArray() {
    ByteBuffer[] array = new ByteBuffer[cache.length];
    ListIterator<ByteBuffer> iter = listIterator(getMinimumAvailableIndex());
    int j = 0;
    while (iter.hasNext()) {
      array[j] = iter.next().duplicate();
      j++;
    }
    return array;
  }

  @SuppressWarnings("unchecked")
  public <T> T[] toArray(T[] a) {
    // public ByteBuffer[] toArray(ByteBuffer[] a) {
    int minIndex = Math.max(getMinimumAvailableIndex(), getMaximumAvailableIndex() - a.length);
    ListIterator<java.nio.ByteBuffer> iter = listIterator(minIndex);
    int j = 0;
    while (iter.hasNext()) {
      a[j] = (T) iter.next().duplicate();
      j++;
    }
    for (; j < a.length; j++) {
      a[j] = (T) ByteBuffer.allocate(0);
    }
    return a;
  }

  private static void copyBuffer(ByteBuffer src, ByteBuffer dest) {
    dest.clear();
    ByteBuffer dup = src.duplicate();
    dest.put(dup);
    dest.flip();
  }

  private ByteBuffer getElement(int position) {
    return cache[position];
  }

  private int getMaximumAvailableIndex() {
    return maxIndex;
  }

  private int getMinimumAvailableIndex() {
    if (maxIndex > cache.length) {
      return maxIndex - cache.length + 1;
    } else if (maxIndex >= 0) {
      return 0;
    } else {
      return -1;
    }
  }

  private int position(int index) {
    return index % cache.length;
  }

}
