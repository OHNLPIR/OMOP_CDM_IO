package org.ohnlp.ir.emirs.model.queryeditor;

import java.util.Collection;

public class CDMQuery {
    public Collection<CDMObject> getObjects() {
        return objects;
    }

    public void setObjects(Collection<CDMObject> objects) {
        this.objects = objects;
    }

    Collection<CDMObject> objects;
}
