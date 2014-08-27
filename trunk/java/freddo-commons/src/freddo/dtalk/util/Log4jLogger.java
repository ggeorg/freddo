package freddo.dtalk.util;

import freddo.dtalk.util.LOG.Logger;

public class Log4JLogger implements Logger {

	@Override
	public void d(String tag, String msg) {
		org.apache.log4j.Logger.getLogger(tag).debug(msg);
	}

	@Override
	public void d(String tag, String msg, Throwable t) {
		org.apache.log4j.Logger.getLogger(tag).debug(msg, t);
	}

	@Override
	public void e(String tag, String msg) {
		org.apache.log4j.Logger.getLogger(tag).error(msg);
	}

	@Override
	public void e(String tag, String msg, Throwable t) {
		org.apache.log4j.Logger.getLogger(tag).error(msg, t);
	}

	@Override
	public void i(String tag, String msg) {
		org.apache.log4j.Logger.getLogger(tag).info(msg);
	}

	@Override
	public void i(String tag, String msg, Throwable t) {
		org.apache.log4j.Logger.getLogger(tag).info(msg, t);
	}

	@Override
	public void v(String tag, String msg) {
		org.apache.log4j.Logger.getLogger(tag).trace(msg);
	}

	@Override
	public void v(String tag, String msg, Throwable t) {
		org.apache.log4j.Logger.getLogger(tag).trace(msg, t);
	}

	@Override
	public void w(String tag, String msg) {
		org.apache.log4j.Logger.getLogger(tag).warn(msg);
	}

	@Override
	public void w(String tag, String msg, Throwable t) {
		org.apache.log4j.Logger.getLogger(tag).warn(msg, t);
	}

	@Override
	public void wtf(String tag, String msg) {
		org.apache.log4j.Logger.getLogger(tag).fatal(msg);
	}

	@Override
	public void wtf(String tag, String msg, Throwable t) {
		org.apache.log4j.Logger.getLogger(tag).fatal(msg, t);
	}

	@Override
	public String tag(Class<?> cls) {
		return cls.getName();
	}

}
