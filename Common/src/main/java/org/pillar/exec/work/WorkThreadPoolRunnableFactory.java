package org.pillar.exec.work;

public interface WorkThreadPoolRunnableFactory<W> {
	WorkThreadPoolRunnable<W> createRunnable();
}
