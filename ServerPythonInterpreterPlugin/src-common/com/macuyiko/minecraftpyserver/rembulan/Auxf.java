package com.macuyiko.minecraftpyserver.rembulan;

/*
 * Copyright 2016 Miroslav Janíček
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.sandius.rembulan.impl.NonsuspendableFunctionException;
import net.sandius.rembulan.runtime.AbstractFunction0;
import net.sandius.rembulan.runtime.AbstractFunction2;
import net.sandius.rembulan.runtime.AbstractFunction3;
import net.sandius.rembulan.runtime.AbstractFunctionAnyArg;
import net.sandius.rembulan.runtime.Dispatch;
import net.sandius.rembulan.runtime.ExecutionContext;
import net.sandius.rembulan.runtime.LuaFunction;
import net.sandius.rembulan.runtime.ResolvedControlThrowable;
import net.sandius.rembulan.runtime.UnresolvedControlThrowable;

import java.util.Arrays;
import java.util.Objects;

public final class Auxf {

	private Auxf() {
		// not to be instantiated
	}

	/**
	 * Returns a vararg function that calls the result of the Lua expression {@code x()}
	 * with the remaining supplied arguments.
	 *
	 * <p>In Lua terms, the returned function is equivalent to:</p>
	 * <pre>
	 *   function (...)
	 *     return x()(...)
	 *   end
	 * </pre>
	 * <p>where {@code x} is the argument supplied to this method.</p>
	 *
	 * @param x  the argument to evaluate as a function, may be {@code null}
	 * @return  a vararg function that calls the result of {@code x()} with the remaining
	 *          arguments
	 */
	public static LuaFunction lift(Object x) {
		return new Lift(x);
	}

	/**
	 * Returns a vararg function that returns the results of the call {@code x(y1,...,yn, ...)},
	 * where {@code yi} is the {@code i-th} value in {@code ys}. In other words, the function
	 * inserts {@code ys} to the argument list before the vararg arguments, and calls {@code f}
	 * with the resulting arguments.
	 *
	 * <p>For illustration, consider the case in which {@code ys} consists of two values
	 * {@code y1} and {@code y2}. Then the function returned by this method this is equivalent
	 * to:</p>
	 * <pre>
	 *   function (...)
	 *     return x(y1, y2, ...)
	 *   end
	 * </pre>
	 *
	 * <p>For empty {@code ys}, the resulting function is equivalent to:</p>
	 * <pre>
	 *   function (...)
	 *     return x(...)
	 *   end
	 * </pre>
	 *
	 * @param x  the call target, may be {@code null}
	 * @param ys  the arguments to insert to the argument list, must not be {@code null}
	 * @return  a vararg function that inserts {@code ys} to the argument list and calls {@code x}
	 *
	 * @throws NullPointerException  if {@code ys} is {@code null}
	 */
	public static LuaFunction bind(Object x, Object... ys) {
		return new Bind(x, Arrays.copyOf(ys, ys.length));
	}

	/**
	 * Returns a 0-ary function that returns the results of the call {@code x(y1,...,yn)},
	 * where {@code yi} is the {@code i}-th value in {@code ys}.
	 *
	 * <p>For example, if {@code ys} consists of two values {@code y1} and {@code y2},
	 * the returned function is equivalent to:</p>
	 * <pre>
	 *   function ()
	 *     return x(y1, y2)
	 *   end
	 * </pre>
	 *
	 * @param x  the call target, may be {@code null}
	 * @param ys  the arguments to call {@code x} with, must not be {@code null}
	 * @return  a 0-ary function that returns the results of {@code x(ys)}
	 *
	 * @throws NullPointerException  if {@code ys} is {@code null}
	 */
	public static LuaFunction call(Object x, Object... ys) {
		return new Call(bind(x, ys));
	}

	/**
	 * Returns a function of two arguments {@code t} and {@code k} that returns the result
	 * of the Lua expression {@code t[k]}.
	 *
	 * <p>In other words, the returned function is equivalent to:</p>
	 * <pre>
	 *   function (t, k)
	 *     return t[k]
	 *   end
	 * </pre>
	 *
	 * @return  a function of two arguments that performs a table lookup
	 */
	public static LuaFunction index() {
		return Index.INSTANCE;
	}

	/**
	 * Returns a 0-ary function that returns the result of the Lua expression {@code t[k]},
	 * including metamethod processing.
	 *
	 * <p>In other words, the returned function is equivalent to:</p>
	 * <pre>
	 *   function ()
	 *     return t[k]
	 *   end
	 * </pre>
	 * <p>where {@code t} and {@code k} are the arguments supplied to this method.</p>
	 *
	 * @param t  the object to index, may be {@code null}
	 * @param k  the key to look up in {@code t}, may be {@code null}
	 * @return  a function that returns the result of {@code t[k]}
	 */
	public static LuaFunction index(Object t, Object k) {
		return call(index(), t, k);
	}

	/**
	 * Returns a function of three arguments {@code t}, {@code k} and {@code v} that
	 * executes the Lua statement {@code t[k] = v}.
	 *
	 * <p>In Lua terms, the returned function is equivalent to:</p>
	 * <pre>
	 *   function (t, k, v)
	 *     t[k] = v
	 *   end
	 * </pre>
	 *
	 * @return  a ternary function that performs table assignment
	 */
	public static LuaFunction setIndex() {
		return SetIndex.INSTANCE;
	}

	/**
	 * Returns a 0-ary function that executes the Lua statement {@code t[k] = v}.
	 *
	 * <p>In Lua terms, the returned function is equivalent to:</p>
	 * <pre>
	 *   function ()
	 *     t[k] = v
	 *   end
	 * </pre>
	 * <p>where {@code t}, {@code k} and {@code v} are the arguments to this method.</p>
	 *
	 * @param t  the object to assign to, may be {@code null}
	 * @param k  the key to assign, may be {@code null}
	 * @param v  the value to assign, may be {@code null}
	 * @return  a 0-ary function that performs table assignment
	 */
	public static LuaFunction setIndex(Object t, Object k, Object v) {
		return call(setIndex(), t, k, v);
	}

	/**
	 * Returns a new vararg function that looks up {@code name} in {@code env},
	 * and calls the result of the lookup with the supplied arguments. Neither operation is
	 * raw, i.e., it both the lookup and call may involve metamethod processing.
	 *
	 * <p>In Lua terms, the returned function is equivalent to:</p>
	 * <pre>
	 *   function (...)
	 *     return env[name](...)
	 *   end
	 * </pre>
	 * <p>where {@code env} and {@code name} are the arguments supplied to this method.</p>
	 *
	 * @param env  the environment to look up {@code name} in, may be {@code null}
	 * @param name  the name to look up in {@code env}, may be {@code null}
	 * @return  a vararg function that looks up its first argument in {@code env} and calls it
	 *          with the remaining arguments
	 */
	public static LuaFunction callGlobal(Object env, Object name) {
		return lift(index(env, name));
	}


	static class Lift extends AbstractFunctionAnyArg {

		private final Object x;

		public Lift(Object x) {
			this.x = x;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Lift that = (Lift) o;
			return Objects.equals(x, that.x);
		}

		@Override
		public int hashCode() {
			return Objects.hash(getClass(), x);
		}

		@Override
		public void invoke(ExecutionContext context, Object[] args) throws ResolvedControlThrowable {
			try {
				Dispatch.call(context, x);
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, Arrays.copyOf(args, args.length));
			}

			resume(context, args);
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			Object target = context.getReturnBuffer().get0();
			context.getReturnBuffer().setToCallWithContentsOf(target, (Object[]) suspendedState);
		}

	}

	static class Bind extends AbstractFunctionAnyArg {

		private final Object fn;
		private final Object[] curriedArgs;

		public Bind(Object fn, Object[] args) {
			this.fn = fn;  // may be null
			this.curriedArgs = Objects.requireNonNull(args);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Bind that = (Bind) o;
			return Objects.equals(fn, that.fn) && Arrays.equals(curriedArgs, that.curriedArgs);
		}

		@Override
		public int hashCode() {
			int result = getClass().hashCode();
			result = 31 * result + (fn != null ? fn.hashCode() : 0);
			result = 31 * result + Arrays.hashCode(curriedArgs);
			return result;
		}

		@Override
		public void invoke(ExecutionContext context, Object[] args) throws ResolvedControlThrowable {
			Object[] callArgs = new Object[curriedArgs.length + args.length];
			System.arraycopy(curriedArgs, 0, callArgs, 0, curriedArgs.length);
			System.arraycopy(args, 0, callArgs, curriedArgs.length, args.length);
			context.getReturnBuffer().setToCallWithContentsOf(fn, callArgs);
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			throw new NonsuspendableFunctionException(getClass());
		}

	}

	static class Call extends AbstractFunction0 {

		private final Object x;

		public Call(Object x) {
			this.x = x;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Call that = (Call) o;
			return Objects.equals(x, that.x);
		}

		@Override
		public int hashCode() {
			return Objects.hash(getClass(), x);
		}

		@Override
		public void invoke(ExecutionContext context) throws ResolvedControlThrowable {
			context.getReturnBuffer().setToCall(x);
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			throw new NonsuspendableFunctionException(this.getClass());
		}

	}

	static class Index extends AbstractFunction2 {

		static final Index INSTANCE = new Index();

		@Override
		public void invoke(ExecutionContext context, Object t, Object k) throws ResolvedControlThrowable {
			try {
				Dispatch.index(context, t, k);
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, null);
			}

			resume(context, null);
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			Object result = context.getReturnBuffer().get0();
			context.getReturnBuffer().setTo(result);
		}

	}

	static class SetIndex extends AbstractFunction3 {

		static final SetIndex INSTANCE = new SetIndex();

		@Override
		public void invoke(ExecutionContext context, Object t, Object k, Object v) throws ResolvedControlThrowable {
			try {
				Dispatch.setindex(context, t, k, v);
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, null);
			}

			resume(context, null);
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			context.getReturnBuffer().setTo();
		}

	}

}