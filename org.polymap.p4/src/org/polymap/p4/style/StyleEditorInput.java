/*
 * polymap.org Copyright (C) 2016, the @authors. All rights reserved.
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
package org.polymap.p4.style;

import org.geotools.data.FeatureStore;
import org.opengis.feature.type.FeatureType;

/**
 * Container to transport relevant data to the style panel.
 * 
 * @author Steffen Stundzig
 */
public class StyleEditorInput {

    private final String       styleIdentifier;

    private final FeatureType  featureType;

    private final FeatureStore featureStore;


    public StyleEditorInput( final String styleIdentifier, final FeatureStore featureStore ) {
        this.styleIdentifier = styleIdentifier;
        this.featureType = featureStore.getSchema();
        this.featureStore = featureStore;
    }


    public StyleEditorInput( final String styleIdentifier, final FeatureType featureType ) {
        this.styleIdentifier = styleIdentifier;
        this.featureType = featureType;
        this.featureStore = null;
    }


    public String styleIdentifier() {
        return styleIdentifier;
    }


    public FeatureType featureType() {
        return featureType;
    }


    public FeatureStore featureStore() {
        return featureStore;
    }
}
