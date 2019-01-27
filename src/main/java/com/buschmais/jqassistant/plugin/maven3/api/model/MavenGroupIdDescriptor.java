package com.buschmais.jqassistant.plugin.maven3.api.model;

import java.util.List;

import com.buschmais.jqassistant.plugin.common.api.model.NamedDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("GroupId")
public interface MavenGroupIdDescriptor extends MavenDescriptor, NamedDescriptor {

    @Relation("CONTAINS")
    List<MavenArtifactIdDescriptor> getArtifactIds();

}