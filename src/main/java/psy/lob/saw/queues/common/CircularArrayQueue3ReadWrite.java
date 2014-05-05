package psy.lob.saw.queues.common;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode({ Mode.AverageTime })
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
public class CircularArrayQueue3ReadWrite {
	public static final int CAPACITY = 1 << 15;
	public static final Integer TOKEN = 1;

	private CircularArrayQueue3<Integer> caq = new CircularArrayQueue3<Integer>(CAPACITY) {

		@Override
		public Integer poll() {
			return null;
		}

		@Override
		public Integer peek() {
			return null;
		}

		@Override
		public boolean offer(Integer e) {
			return false;
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public Iterator<Integer> iterator() {
			return null;
		}
	};

	long index;

	@GenerateMicroBenchmark
	public void offer() {
		long offset = caq.calcOffset(index++);
		caq.SP_element(offset, TOKEN);
	}

	@GenerateMicroBenchmark
	public void poll() {
		long offset = caq.calcOffset(index++);
		if (caq.LP_element(offset) != null) {
			index--;
		}
	}
}