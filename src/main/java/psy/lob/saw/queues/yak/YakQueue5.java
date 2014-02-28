/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package psy.lob.saw.queues.yak;

import java.util.AbstractQueue;
import java.util.Iterator;

import psy.lob.saw.util.Pow2;
import psy.lob.saw.util.UnsafeAccess;

/**
 * <ul>
 * <li>Inlined counters
 * <li>Counters are padded
 * <li>Data is padded
 * <li>Class is pre-padded
 * <li>Use Unsafe for array access
 * </ul>
 */
abstract class YakQueue5L0Pad<E> extends AbstractQueue<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;
}

abstract class YakQueue5ColdFields<E> extends YakQueue5L0Pad<E> {
	protected static final int BUFFER_PAD = 16;
	protected final int capacity;
	protected final long mask;
	protected final E[] buffer;

	@SuppressWarnings("unchecked")
	public YakQueue5ColdFields(int capacity) {
		if (Pow2.isPowerOf2(capacity)) {
			this.capacity = capacity;
		} else {
			this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
		}
		mask = this.capacity - 1;
		buffer = (E[]) new Object[this.capacity + BUFFER_PAD * 2];
	}
}

abstract class YakQueue5L1Pad<E> extends YakQueue5ColdFields<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;

	public YakQueue5L1Pad(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue5TailField<E> extends YakQueue5L1Pad<E> {
	protected volatile long tail;

	public YakQueue5TailField(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue5L2Pad<E> extends YakQueue5TailField<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;

	public YakQueue5L2Pad(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue5HeadCache<E> extends YakQueue5L2Pad<E> {
	protected long headCache;

	public YakQueue5HeadCache(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue5L3Pad<E> extends YakQueue5HeadCache<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;

	public YakQueue5L3Pad(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue5HeadField<E> extends YakQueue5L3Pad<E> {
	protected volatile long head;

	public YakQueue5HeadField(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue5L4Pad<E> extends YakQueue5HeadField<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;

	public YakQueue5L4Pad(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue5TailCache<E> extends YakQueue5L4Pad<E> {
	protected long tailCache;

	public YakQueue5TailCache(int capacity) {
		super(capacity);
	}

}

abstract class YakQueue5L5Pad<E> extends YakQueue5TailCache<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;

	public YakQueue5L5Pad(int capacity) {
		super(capacity);
	}
}

public final class YakQueue5<E> extends YakQueue5L5Pad<E> {
	private static final long TAIL_OFFSET;
	private static final long HEAD_OFFSET;
	private static final long ARRAY_BASE;
	private static final int ELEMENT_SHIFT;
	static {
		try {
			TAIL_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(YakQueue5TailField.class.getDeclaredField("tail"));
			HEAD_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(YakQueue5HeadField.class.getDeclaredField("head"));
			final int scale = UnsafeAccess.UNSAFE.arrayIndexScale(Object[].class);

			if (4 == scale) {
				ELEMENT_SHIFT = 2;
			} else if (8 == scale) {
				ELEMENT_SHIFT = 3;
			} else {
				throw new IllegalStateException("Unknown pointer size");
			}
			ARRAY_BASE = UnsafeAccess.UNSAFE.arrayBaseOffset(Object[].class) + (BUFFER_PAD << ELEMENT_SHIFT);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	public YakQueue5(final int capacity) {
		super(capacity);
	}

	@Override
	public boolean offer(final E e) {
		if (null == e) {
			throw new NullPointerException("Null is not a valid element");
		}

		final long currentTail = getTail();
		final long wrapPoint = currentTail - capacity;
		if (headCache <= wrapPoint) {
			headCache = getHead();
			if (headCache <= wrapPoint) {
				return false;
			}
		}
		UnsafeAccess.UNSAFE.putObject(buffer, offset(currentTail), e);
		tailLazySet(currentTail + 1);

		return true;
	}

	@Override
	public E poll() {
		final long currentHead = getHead();
		if (currentHead >= tailCache) {
			tailCache = getTail();
			if (currentHead >= tailCache) {
				return null;
			}
		}

		final long offset = offset(currentHead);
		@SuppressWarnings("unchecked")
		final E e = (E) UnsafeAccess.UNSAFE.getObject(buffer, offset);
		UnsafeAccess.UNSAFE.putObject(buffer, offset, null);
		headLazySet(currentHead + 1);

		return e;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E peek() {
		return (E) UnsafeAccess.UNSAFE.getObject(buffer, offset(getHead()));
	}

	@Override
	public int size() {
		return (int) (getTail() - getHead());
	}

	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}

	private void headLazySet(long v) {
		UnsafeAccess.UNSAFE.putOrderedLong(this, HEAD_OFFSET, v);
	}

	private long getHead() {
		return head;
	}

	private void tailLazySet(long v) {
		UnsafeAccess.UNSAFE.putOrderedLong(this, TAIL_OFFSET, v);
	}

	private long getTail() {
		return tail;
	}

	private long offset(long index) {
		return ARRAY_BASE + ((index & mask) << ELEMENT_SHIFT);
	}
}