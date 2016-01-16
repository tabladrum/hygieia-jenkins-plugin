package jenkins.plugins.hygieia;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.tasks.Publisher;

import java.util.Map;
import java.util.logging.Logger;

@Extension
@SuppressWarnings("rawtypes")
public class HygieiaListener extends RunListener<AbstractBuild> {

    private static final Logger logger = Logger.getLogger(HygieiaListener.class.getName());

    public HygieiaListener() {
        super(AbstractBuild.class);
    }

    @Override
    public void onCompleted(AbstractBuild r, TaskListener listener) {
        logger.info("Build completed");
        getNotifier(r.getProject(), listener).completed(r);
        super.onCompleted(r, listener);
    }

    @Override
    public void onStarted(AbstractBuild r, TaskListener listener) {
        logger.info("Build Started");
        // getNotifier(r.getProject()).started(r);
        // super.onStarted(r, listener);
    }

    @Override
    public void onDeleted(AbstractBuild r) {
        // getNotifier(r.getProject()).deleted(r);
        // super.onDeleted(r);
    }

    @Override
    public void onFinalized(AbstractBuild r) {
        // getNotifier(r.getProject()).finalized(r);
        // super.onFinalized(r);
        logger.info("Build finalized");
    }

    @SuppressWarnings("unchecked")
    FineGrainedNotifier getNotifier(AbstractProject project, TaskListener listener) {
        Map<Descriptor<Publisher>, Publisher> map = project.getPublishersList().toMap();
        for (Publisher publisher : map.values()) {
            if (publisher instanceof HygieiaPublisher) {
                logger.info("Getting Hygieia notifier");
                return new ActiveNotifier((HygieiaPublisher) publisher, (BuildListener)listener);
            }
        }
        return new DisabledNotifier();
    }

}
