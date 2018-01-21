package com.github.bamirov.pulse.jdbc;

import com.github.bamirov.pillar.time.interfaces.Timestamp;
import com.github.bamirov.pulse.interfaces.ServerPulseRecord;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class JDBCServerPulseRecord implements ServerPulseRecord<Long> {
	@Getter
	protected Long serverId;
	@Getter
	protected String info;
	@Getter
	protected Timestamp creationTime;
	@Getter
	protected Timestamp lastHBTime;
	@Getter
	protected long HBPeriodMs;
}
