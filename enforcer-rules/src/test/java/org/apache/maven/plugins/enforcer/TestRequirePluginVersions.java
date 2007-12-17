package org.apache.maven.plugins.enforcer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugins.enforcer.utils.EnforcerRuleUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class TestRequirePluginVersions
    extends AbstractMojoTestCase
{

    public void testHasVersionSpecified()
    {
        Plugin source = new Plugin();
        source.setArtifactId( "foo" );
        source.setGroupId( "group" );

        // setup the plugins. I'm setting up the foo group
        // with a few bogus entries and then a real one.
        // this is to test that the list is exhaustively
        // searched for versions before giving up.
        // banLatest/Release will fail if it is found
        // anywhere in the list
        List plugins = new ArrayList();
        plugins.add( EnforcerTestUtils.newPlugin( "group", "a-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", null ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", "" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "b-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "c-artifact", "LATEST" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "c-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "d-artifact", "RELEASE" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "d-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "e-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "e-artifact", "RELEASE" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "f-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "f-artifact", "LATEST" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "f-artifact", "1.0-SNAPSHOT" ) );

        RequirePluginVersions rule = new RequirePluginVersions();
        rule.setBanLatest( false );
        rule.setBanRelease( false );
        rule.setBanSnapshots( false );

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();

        assertTrue( rule.hasValidVersionSpecified( helper, source, plugins ) );

        // check that LATEST is allowed
        source.setArtifactId( "c-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, plugins ) );

        // check that LATEST is banned
        rule.setBanLatest( true );
        assertFalse( rule.hasValidVersionSpecified( helper, source, plugins ) );

        // check that LATEST is exhausively checked
        rule.setBanSnapshots( false );
        source.setArtifactId( "f-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, plugins ) );

        rule.setBanLatest( false );
        rule.setBanSnapshots( true );
        assertFalse( rule.hasValidVersionSpecified( helper, source, plugins ) );

        // check that RELEASE is allowed
        source.setArtifactId( "d-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, plugins ) );

        // check that RELEASE is banned
        rule.setBanRelease( true );
        assertFalse( rule.hasValidVersionSpecified( helper, source, plugins ) );

        // check that RELEASE is exhaustively checked
        source.setArtifactId( "e-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, plugins ) );
    }

    
    public void testHasVersionSpecifiedWithProperties()
    {
        Plugin source = new Plugin();
        source.setGroupId( "group" );

        // setup the plugins. 
        List plugins = new ArrayList();
        plugins.add( EnforcerTestUtils.newPlugin( "group", "a-artifact", "1.0-${SNAPSHOT}" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "b-artifact", "${1.0}" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "c-artifact", "${LATEST}" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "d-artifact", "${RELEASE}" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "e-artifact", "${}" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "f-artifact", "${   }" ) );
        

        RequirePluginVersions rule = new RequirePluginVersions();
        rule.setBanLatest( false );
        rule.setBanRelease( false );
        rule.setBanSnapshots( false );

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper(true);

        source.setArtifactId( "a-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, plugins ) );

        source.setArtifactId( "b-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, plugins ) );
        
        source.setArtifactId( "c-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, plugins ) );
        
        source.setArtifactId( "d-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, plugins ) );

        //this one checks empty property values
        source.setArtifactId( "e-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, plugins ) );
        
        // this one checks empty property values
        source.setArtifactId( "f-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, plugins ) );
        
        rule.setBanLatest( true );
        source.setArtifactId( "c-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, plugins ) );
        
        rule.setBanRelease( true );
        source.setArtifactId( "d-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, plugins ) );
        
        rule.setBanSnapshots( true );
        source.setArtifactId( "a-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, plugins ) );
        
        //release versions should pass everything
        source.setArtifactId( "b-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, plugins ) );
    }

    
    public void testGetAllPlugins()
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        RequirePluginVersions rule = new RequirePluginVersions();
        String path = "target/test-classes/requirePluginVersions/getPomRecursively/b/c";

        StringUtils.replace( path, "/", File.separator );

        File projectDir = new File( getBasedir(), path );

        MockProject project = new MockProject();
        project.setArtifactId( "c" );
        project.setGroupId( "group" );
        project.setVersion( "1.0" );
        project.setBaseDir( projectDir );

        rule.setUtils( new EnforcerRuleUtils( EnforcerTestUtils.getHelper( project ) ) );
        List plugins = rule.getAllPluginEntries( project );

        // there should be 3
        assertEquals( 3, plugins.size() );
    }

    public void testGetAdditionalPluginsNull()
        throws MojoExecutionException
    {
        RequirePluginVersions rule = new RequirePluginVersions();
        rule.addAdditionalPlugins( null, null );
    }

    public void testGetAdditionalPluginsInvalidFormat()
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        List additional = new ArrayList();

        // invalid format (not enough sections)
        additional.add( "group" );

        Set plugins = new HashSet();
        try
        {
            rule.addAdditionalPlugins( plugins, additional );
            fail( "Expected Exception because the format is invalid" );
        }
        catch ( MojoExecutionException e )
        {
        }

        // invalid format (too many sections)
        additional.clear();
        additional.add( "group:i:i" );
        try
        {
            rule.addAdditionalPlugins( plugins, additional );
            fail( "Expected Exception because the format is invalid" );
        }
        catch ( MojoExecutionException e )
        {
        }

    }

    public void testGetAdditionalPluginsEmptySet()
        throws MojoExecutionException
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        Set plugins = new HashSet();
        plugins.add( EnforcerTestUtils.newPlugin( "group", "a-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", null ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo2", "" ) );

        List additional = new ArrayList();
        additional.add( "group:a-artifact" );
        additional.add( "group:another-artifact" );

        // make sure a null set can be handled
        Set results = rule.addAdditionalPlugins( null, additional );

        assertNotNull( results );
        assertContainsPlugin( "group", "a-artifact", results );
        assertContainsPlugin( "group", "another-artifact", results );

    }

    public void testGetAdditionalPlugins()
        throws MojoExecutionException
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        Set plugins = new HashSet();
        plugins.add( EnforcerTestUtils.newPlugin( "group", "a-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", null ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo2", "" ) );

        List additional = new ArrayList();
        additional.add( "group:a-artifact" );
        additional.add( "group:another-artifact" );

        Set results = rule.addAdditionalPlugins( plugins, additional );

        // make sure only one new plugin has been added
        assertNotNull( results );
        assertEquals( 4, results.size() );
        assertContainsPlugin( "group", "a-artifact", results );
        assertContainsPlugin( "group", "another-artifact", results );

    }

    private void assertContainsPlugin( String group, String artifact, Set theSet )
    {
        Plugin p = new Plugin();
        p.setGroupId( group );
        p.setArtifactId( artifact );
        assertTrue( theSet.contains( p ) );
    }

    public void testId()
    {
        RequirePluginVersions rule = new RequirePluginVersions();
        rule.getCacheId();
    }
}