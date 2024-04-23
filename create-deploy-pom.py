import sys
from string import Template
import os.path

def main():
    # build parameters from the command line arguments
    params_dict = {'ARTIFACT_ID': sys.argv[1],
                   'RELEASE_NAME': sys.argv[2],
                   'PACKAGING':sys.argv[3],
                   'GROUP_ID': 'org.finos.cdm'}
    # add in additional developers if any
    if (len (sys.argv) > 4 and os.path.isfile(sys.argv[4])) :
        developers_file = open(sys.argv[4], 'r') 
        params_dict['DEVELOPERS'] = developers_file.read ()
        developers_file.close()
    else:
        params_dict['DEVELOPERS'] = '''        <developer>
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
        </developer>'''

    deploy_pom_text = '''<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>$GROUP_ID</groupId>
    <artifactId>$ARTIFACT_ID</artifactId>
    <version>$RELEASE_NAME</version>
    <packaging>$PACKAGING</packaging>

    <name>$ARTIFACT_ID</name>

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
'''
    deploy_pom_text = Template(deploy_pom_text).safe_substitute(params_dict)
    deploy_pom_file = open(params_dict['ARTIFACT_ID'] + '-' + params_dict['RELEASE_NAME']+ '.pom', "w")
    deploy_pom_file.write(deploy_pom_text)
    deploy_pom_file.close()

if __name__ == "__main__":
    main()