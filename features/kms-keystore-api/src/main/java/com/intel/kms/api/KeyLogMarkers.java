/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.kms.api;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 *
 * @author jbuhacoff
 */
public class KeyLogMarkers {
    public static Marker CREATE_KEY = MarkerFactory.getMarker("CREATE_KEY");
    public static Marker REGISTER_KEY = MarkerFactory.getMarker("REGISTER_KEY");
    public static Marker DELETE_KEY = MarkerFactory.getMarker("DELETE_KEY");
    public static Marker TRANSFER_KEY = MarkerFactory.getMarker("TRANSFER_KEY");
}
