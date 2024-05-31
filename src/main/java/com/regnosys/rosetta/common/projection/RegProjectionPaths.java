package com.regnosys.rosetta.common.projection;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.regnosys.rosetta.common.RegPaths;
import com.regnosys.rosetta.common.reports.RegReportPaths;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RegProjectionPaths extends RegPaths {

    public static final Path PROJECTION_PATH = Paths.get("projection");
    public static final Path ISO20022_PATH = Paths.get("iso-20022");

    public RegProjectionPaths(Path rootPath, Path input, Path output, Path config, Path lookup) {
        super(rootPath, input, output, config, lookup);
    }

    public static RegProjectionPaths getProjectionPath() {
        Path projectionPath = PROJECTION_PATH;
        Path isoPath = projectionPath.resolve(ISO20022_PATH);
        return new RegProjectionPaths(isoPath,
                isoPath.resolve(INPUT_PATH),
                isoPath.resolve(OUTPUT_PATH),
                isoPath.resolve(CONFIG_PATH),
                isoPath.resolve(LOOKUP_PATH));
    }
}
