/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   SampleGattAttributes.java
 * @lastmodify 2021/02/26 12:41:21
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

package cz.vsb.cbe.testbed;

import java.util.UUID;

public class SampleGattAttributes {

    public static UUID TESTBED_SERVICE_UUID =
            UUID.fromString("21110001-e780-4112-a997-5c3e475adbc3");
    public static UUID TESTBED_ID_CHARACTERISTIC_UUID =
            UUID.fromString("21110005-e780-4112-a997-5c3e475adbc3");
    public static UUID HEART_RATE_CHARACTERISTIC_UUID =
            UUID.fromString("21110003-e780-4112-a997-5c3e475adbc3");
    public static UUID STEPS_CHARACTERISTIC_UUID =
            UUID.fromString("21110004-e780-4112-a997-5c3e475adbc3");
    public static UUID TEMPERATURE_CHARACTERISTIC_UUID =
            UUID.fromString("21110002-e780-4112-a997-5c3e475adbc3");

    public static UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

}