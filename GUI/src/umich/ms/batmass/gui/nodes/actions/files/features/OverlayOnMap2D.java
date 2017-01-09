/* 
 * Copyright 2016 Dmitry Avtonomov.
 *
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
 */
package umich.ms.batmass.gui.nodes.actions.files.features;

import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import umich.ms.batmass.data.core.lcms.features.data.FeatureTableModelData;
import umich.ms.batmass.filesupport.core.spi.nodes.NodeInfo;
import umich.ms.batmass.gui.viewers.map2d.Map2DTopComponent;
import umich.ms.batmass.nbputils.SwingHelper;
import umich.ms.datatypes.LCMSData;

/**
 * An action which looks for data of 2 types in the global lookup to be activated.
 * Overlays feature data over Map2D.
 * @author Dmitry Avtonomov
 */
@ActionID(
        category = "BatMass/Nodes",
        id = "umich.ms.batmass.gui.nodes.actions.files.features.OverlayOnMap2D"
)
@ActionRegistration(
        displayName = "#CTL_OverlayOnMap2D",
        lazy = false
)
@ActionReferences({
    @ActionReference(
            path = NodeInfo.ACTIONS_LAYER_PATH_BASE + "/batmass-project-any/batmass-type-any/features",
            position = 520
    ),
    @ActionReference(
            path = NodeInfo.ACTIONS_LAYER_PATH_BASE + "/batmass-project-any/batmass-type-any/lcms",
            position = 520
    ),
    @ActionReference(
            path = NodeInfo.ACTIONS_LAYER_PATH_BASE + "/batmass-project-any/batmass-type-any/pep_id",
            position = 520
    ) 
})
@NbBundle.Messages("CTL_OverlayOnMap2D=Feature overlay")
@SuppressWarnings({"rawtypes"})
public class OverlayOnMap2D extends AbstractAction
        implements LookupListener, ContextAwareAction {

    private Lookup context;
    private volatile Lookup.Result<LCMSData> lkpLCMSData;
    private volatile Lookup.Result<FeatureTableModelData> lkpFeaturesData;
    @StaticResource
    private static final String ICON_PATH = "umich/ms/batmass/gui/resources/view_map2d_16.png";
    public static final ImageIcon ICON = ImageUtilities.loadImageIcon(ICON_PATH, false);

    public OverlayOnMap2D() {
        this(Utilities.actionsGlobalContext());
    }

    public OverlayOnMap2D(Lookup context) {
        this.context = context;
        putValue(Action.NAME, Bundle.CTL_OverlayOnMap2D());
        putValue(Action.SMALL_ICON, ICON);
    }

    protected void init() {
        assert SwingUtilities.isEventDispatchThread() : "This shall be called only from AWT thread (EDT)";

        Lookup.Result<LCMSData> lcmsTmp = lkpLCMSData;
        Lookup.Result<FeatureTableModelData> feturesTmp = lkpFeaturesData;
        if (lcmsTmp == null || feturesTmp == null) {
            synchronized (this) {
                lcmsTmp = lkpLCMSData;
                if (lcmsTmp == null) {
                    // The thing we want to listen for the presence or absence of
                    // in the global selection
                    lcmsTmp = context.lookupResult(LCMSData.class);
                    lkpLCMSData = lcmsTmp;
                    lcmsTmp.addLookupListener(this);
                    resultChanged(null);
                }
                feturesTmp = lkpFeaturesData;
                if (feturesTmp == null) {
                    feturesTmp = context.lookupResult(FeatureTableModelData.class);
                    lkpFeaturesData = feturesTmp;
                    feturesTmp.addLookupListener(this);
                    resultChanged(null);
                }
            }
        }
    }

    @Override
    public boolean isEnabled() {
        init();
        return lkpLCMSData.allInstances().size() == 1 && lkpFeaturesData.allInstances().size() == 1;
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnabled(isEnabled());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new OverlayOnMap2D(actionContext);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<? extends LCMSData> resultLCMS = lkpLCMSData.allInstances();
        if (resultLCMS.size() != 1) {
            throw new IllegalStateException("This action can only act when there"
                    + "is exactly one LCMSData object in global selection.");
        }
        Collection<? extends FeatureTableModelData> resultFeatures = lkpFeaturesData.allInstances();
        if (resultFeatures.size() != 1) {
            throw new IllegalStateException("This action can only act when there"
                    + "is exactly one FeatureTableModelData object in global selection.");
        }
        
        final LCMSData data = resultLCMS.iterator().next();
        final FeatureTableModelData<?> dataFeatures = resultFeatures.iterator().next();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final Map2DTopComponent tc = new Map2DTopComponent();
                tc.open();
                tc.setData(data);
                tc.addData(dataFeatures);
            }
        };
        SwingHelper.invokeOnEDT(runnable);
    }

}
