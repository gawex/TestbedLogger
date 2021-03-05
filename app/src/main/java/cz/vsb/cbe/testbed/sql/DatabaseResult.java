/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   DatabaseResult.java
 * @lastmodify 2021/03/05 12:01:02
 * @verbatim
----------------------------------------------------------------------
Copyright (C) Bc. Lukas Tatarin, 2021

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

<http://www.gnu.org/licenses/>
 @endverbatim
 */

package cz.vsb.cbe.testbed.sql;

public abstract class DatabaseResult {

    public static final class Success<T> extends DatabaseResult {
        public final T data;

        public Success(T data) {
            this.data = data;
        }
    }

    public static final class Error extends DatabaseResult {
        public final Exception exception;

        public Error(Exception exception) {
            this.exception = exception;
        }
    }
}
