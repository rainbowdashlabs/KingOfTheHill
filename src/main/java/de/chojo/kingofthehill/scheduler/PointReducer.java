/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2022 RainbowDashLabs and Contributor
 */

package de.chojo.kingofthehill.scheduler;

import de.chojo.kingofthehill.config.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Deprecated
public class PointReducer implements Runnable {
    private Configuration configuration;

    public PointReducer(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void run() {
        configuration.reduce();
        configuration.save();
        log.debug("Reduced Points.");
    }
}
