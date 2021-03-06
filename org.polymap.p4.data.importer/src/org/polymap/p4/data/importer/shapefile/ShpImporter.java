/*
 * polymap.org 
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.p4.data.importer.shapefile;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FilenameUtils.getExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Composite;

import org.geotools.data.Query;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.feature.simple.SimpleFeatureType;

import org.polymap.core.runtime.Streams;
import org.polymap.core.runtime.Streams.ExceptionCollector;

import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;

import org.polymap.p4.data.importer.ContextIn;
import org.polymap.p4.data.importer.ContextOut;
import org.polymap.p4.data.importer.Importer;
import org.polymap.p4.data.importer.ImporterPlugin;
import org.polymap.p4.data.importer.ImporterSite;
import org.polymap.p4.data.importer.prompts.CharsetPrompt;
import org.polymap.p4.data.importer.prompts.CrsPrompt;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class ShpImporter
        implements Importer {

    private static Log log = LogFactory.getLog( ShpImporter.class );
    
    private static final ShapefileDataStoreFactory dsFactory = new ShapefileDataStoreFactory();
    
    private ImporterSite            site;

    @ContextIn
    protected List<File>            files;

    @ContextIn
    protected File                  shp;

    @ContextOut
    private FeatureCollection       features;

    private Exception               exception;

    private ShapefileDataStore      ds;

    private CharsetPrompt           charsetPrompt;

    private CrsPrompt               crsPrompt;


    @Override
    public ImporterSite site() {
        return site;
    }


    @Override
    public void init( ImporterSite site, IProgressMonitor monitor ) {
        this.site = site;
        site.icon.set( ImporterPlugin.images().svgImage( "shp.svg", SvgImageRegistryHelper.NORMAL24 ) );
        site.summary.set( "Shapefile: " + shp.getName() );
        site.description.set( "A Shapefile is a common file format which contains features of the same type." );
        site.terminal.set( true );
    }


    @Override
    public void createPrompts( IProgressMonitor monitor ) throws Exception {
        charsetPrompt = new CharsetPrompt( site, "Content encoding", "The encoding of the feature content. If unsure use ISO-8859-1.", () -> {
            Charset crs = null;
            try (ExceptionCollector<RuntimeException> exc = Streams.exceptions()) {
                crs = Charset.forName( files.stream()
                        .filter( f -> "cpg".equalsIgnoreCase( getExtension( f.getName() ) ) ).findAny()
                        .map( f -> exc.check( () -> readFileToString( f ).trim() ) )
                        .orElse( CharsetPrompt.DEFAULT.name() ) );
            }
            return crs;
        } );
        crsPrompt = new CrsPrompt( site, "CRS", "The Coordinate Reference System.", () -> {
            Optional<File> prjFile = files.stream().filter( f -> "prj".equalsIgnoreCase( FilenameUtils.getExtension( f.getName() ) ) ).findAny();
            if (prjFile.isPresent()) {
                try {
                    // encoding used in geotools' PrjFileReader
                    String wkt = FileUtils.readFileToString( prjFile.get(), Charset.forName( "ISO-8859-1" ) );
                    return ReferencingFactoryFinder.getCRSFactory( null ).createFromWKT( wkt );
                }
                catch (Exception e) {
                    throw new RuntimeException( e );
                }
            }
            return null;
        } );
    }


    @Override
    public void verify( IProgressMonitor monitor ) {
        try {
            if (ds != null) {
                ds.dispose();
            }
            Map<String,Serializable> params = new HashMap<String,Serializable>();
            params.put( "url", shp.toURI().toURL() );
            params.put( "create spatial index", Boolean.TRUE );

            ds = (ShapefileDataStore)dsFactory.createNewDataStore( params );
            ds.setCharset( charsetPrompt.selection() );
            ds.forceSchemaCRS( crsPrompt.selection() );
            
            Query query = new Query();
            query.setMaxFeatures( 100 );
            features = ds.getFeatureSource().getFeatures( query );

            site.ok.set( true );
            exception = null;
        }
        catch (Exception e) {
            site.ok.set( false );
            exception = e;
        }
    }


    @Override
    public void createResultViewer( Composite parent, IPanelToolkit tk ) {
        if (exception != null) {
            tk.createFlowText( parent, "\nUnable to read the data.\n\n**Reason**: " + exception.getMessage() );
        }
        else {
            SimpleFeatureType schema = (SimpleFeatureType)features.getSchema();
            //log.info( "Features: " + features.size() + " : " + schema.getTypeName() );
            // tk.createFlowText( parent, "Features: *" + features.size() + "*" );
            ShpFeatureTableViewer table = new ShpFeatureTableViewer( parent, schema );
            table.setContent( features );
        }
    }


    @Override
    public void execute( IProgressMonitor monitor ) throws Exception {
        // no maxResults restriction
        features = ds.getFeatureSource().getFeatures();
    }

}
