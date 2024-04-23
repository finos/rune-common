#!/bin/bash
ARTIFACT_ID=$1
RELEASE_NAME=$2
PACKAGING=$3

FILENAME=${ARTIFACT_ID}-${RELEASE_NAME}.pom
GROUP_ID=org.finos.cdm
VERSION=${RELEASE_NAME}

cat > ${FILENAME} <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>${GROUP_ID}</groupId>
    <artifactId>${ARTIFACT_ID}</artifactId>
    <version>${VERSION}</version>
    <packaging>${PACKAGING}</packaging>

    <name>${ARTIFACT_ID}</name>

    <url>https://github.com/REGnosys/rosetta-common</url>

    <scm>
        <developerConnection>scm:git:https://github.com/REGnosys/rosetta-common</developerConnection>
        <connection>scm:git:git://github.com/REGnosys/rosetta-common.git</connection>
        <tag>HEAD</tag>
        <url>https://github.com/REGnosys/rosetta-common</url>
    </scm>

    <licenses>
    		<license>
    			<name>Apache License, Version 2.0</name>
    			<url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    			<distribution>repo</distribution>
    		</license>
    	</licenses>

    <description>Rosetta Common is a java library that is utilised by Rosetta Code Generators and models expressed in the Rosetta DSL.</description>

    <organization>
    		<name>REGnosys</name>
    		<url>https://regnosys.com/</url>
    	</organization>

    <developers>
        <developer>
            <id>minesh-s-patel</id>
            <name>Minesh Patel</name>
            <email>infra@regnosys.com</email>
            <url>http://github.com/minesh-s-patel</url>
            <organization>REGnosys</organization>
            <organizationUrl>https://regnosys.com</organizationUrl>
            <timezone>+1</timezone>
            <roles>
                <role>Maintainer</role>
                <role>Developer</role>
            </roles>
        </developer>
        <developer>
            <id>hugohills-regnosys</id>
            <name>Hugo Hills</name>
            <email>infra@regnosys.com</email>
            <url>http://github.com/hugohills-regnosys</url>
            <organization>REGnosys</organization>
            <organizationUrl>https://regnosys.com</organizationUrl>
            <timezone>+1</timezone>
            <roles>
                <role>Maintainer</role>
                <role>Developer</role>
            </roles>
        </developer>
    </developers>
</project>
EOF
