/*
 * Copyright (c) 2017-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.graphalgo.beta.paths.singlesource;

import org.neo4j.graphalgo.AlgorithmFactory;
import org.neo4j.graphalgo.StreamProc;
import org.neo4j.graphalgo.api.NodeProperties;
import org.neo4j.graphalgo.beta.paths.PathResult;
import org.neo4j.graphalgo.beta.paths.StreamResult;
import org.neo4j.graphalgo.beta.paths.dijkstra.Dijkstra;
import org.neo4j.graphalgo.beta.paths.dijkstra.DijkstraFactory;
import org.neo4j.graphalgo.beta.paths.dijkstra.DijkstraResult;
import org.neo4j.graphalgo.beta.paths.dijkstra.config.AllShortestPathsDijkstraStreamConfig;
import org.neo4j.graphalgo.config.GraphCreateConfig;
import org.neo4j.graphalgo.core.CypherMapWrapper;
import org.neo4j.graphalgo.results.MemoryEstimateResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.neo4j.graphalgo.beta.paths.singlesource.AllShortestPathsDijkstraProc.DIJKSTRA_DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

public class AllShortestPathsDijkstraStreamProc extends StreamProc<Dijkstra, DijkstraResult, StreamResult, AllShortestPathsDijkstraStreamConfig> {

    @Procedure(name = "gds.beta.allShortestPaths.dijkstra.stream", mode = READ)
    @Description(DIJKSTRA_DESCRIPTION)
    public Stream<StreamResult> stream(
        @Name(value = "graphName") Object graphNameOrConfig,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return stream(compute(graphNameOrConfig, configuration));
    }

    @Procedure(name = "gds.beta.allShortestPaths.dijkstra.stream.estimate", mode = READ)
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> streamEstimate(
        @Name(value = "graphName") Object graphNameOrConfig,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return computeEstimate(graphNameOrConfig, configuration);
    }

    @Override
    protected Stream<StreamResult> stream(ComputationResult<Dijkstra, DijkstraResult, AllShortestPathsDijkstraStreamConfig> computationResult) {
        return runWithExceptionLogging("Result streaming failed", () -> {
            var graph = computationResult.graph();
            var config = computationResult.config();

            if (computationResult.isGraphEmpty()) {
                graph.release();
                return Stream.empty();
            }

            var resultBuilder = new StreamResult.Builder(graph, transaction.internalTransaction());
            return computationResult
                .result()
                .paths()
                .takeWhile(path -> path != PathResult.EMPTY)
                .map(path -> resultBuilder.build(path, config.path()));
        });
    }

    @Override
    protected StreamResult streamResult(
        long originalNodeId, long internalNodeId, NodeProperties nodeProperties
    ) {
        throw new UnsupportedOperationException("Dijkstra handles result building individually.");
    }

    @Override
    protected AllShortestPathsDijkstraStreamConfig newConfig(
        String username,
        Optional<String> graphName,
        Optional<GraphCreateConfig> maybeImplicitCreate,
        CypherMapWrapper config
    ) {
        return AllShortestPathsDijkstraStreamConfig.of(username, graphName, maybeImplicitCreate, config);
    }

    @Override
    protected AlgorithmFactory<Dijkstra, AllShortestPathsDijkstraStreamConfig> algorithmFactory() {
        return DijkstraFactory.singleSource();
    }
}

