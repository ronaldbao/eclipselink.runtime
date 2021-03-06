/*******************************************************************************
 * Copyright (c)  2013, 2015  Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     tware - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.internal.jpa.metamodel.proxy;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.eclipse.persistence.internal.jpa.metamodel.SingularAttributeImpl;

public class SingularAttributeProxyImpl<X, T> extends AttributeProxyImpl<X, T> implements SingularAttribute<X, T> {

    @Override
    public javax.persistence.metamodel.Bindable.BindableType getBindableType() {
        return ((SingularAttributeImpl<X, T>)getAttribute()).getBindableType();
    }

    @Override
    public Class<T> getBindableJavaType() {
        return ((SingularAttributeImpl<X, T>)getAttribute()).getBindableJavaType();
    }

    @Override
    public boolean isId() {
        return ((SingularAttributeImpl<X, T>)getAttribute()).isId();
    }

    @Override
    public boolean isVersion() {
        return ((SingularAttributeImpl<X, T>)getAttribute()).isVersion();
    }

    @Override
    public boolean isOptional() {
        return ((SingularAttributeImpl<X, T>)getAttribute()).isOptional();
    }

    @Override
    public Type<T> getType() {
        return ((SingularAttributeImpl<X, T>)getAttribute()).getType();
    }

}
