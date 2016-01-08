/*
 * Copyright 2012-2015 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.animation;

import com.redhat.thermostat.beans.property.DoubleProperty;
import com.redhat.thermostat.beans.property.EnumProperty;
import com.redhat.thermostat.beans.property.ObjectProperty;
import com.redhat.thermostat.common.ThreadPoolTimerFactory;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.event.Event;
import com.redhat.thermostat.event.EventNotifier;

import javax.swing.SwingUtilities;
import java.awt.Toolkit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 */
public class Animation {

    private static final ThreadPoolTimerFactory threadPoolFactory = new ThreadPoolTimerFactory(5);

    public class AnimationEvent extends Event {

        private Status status;
        public AnimationEvent(Object source, Status status) {
            super(source);
            this.status = status;
        }

        public Status getStatus() {
            return status;
        }
    }

    public enum Status {
        PAUSE,
        RUNNING,
        STOPPED,
    }

    public static final double ONE_SECOND = 1000.;
    public static final long DEFAULT_FPS = 60;

    private static final String LOCK = new String("Animation_lock");

    private EnumProperty<Status> statusProperty;
    private DoubleProperty durationProperty;

    private ObjectProperty<EventNotifier<AnimationEvent>> eventNotifierProperty;

    private Interpolator interpolator;

    private List<Clip> clips;

    private Renderer renderer;
    private Starter starter;
    private Stopper stopper;
    private ClipRenderer clipRenderer;

    private Timer timer;

    private double startTime;
    private double lastFrame;

    private volatile double interpolatedFrame;

    public Animation() {
        this(DEFAULT_FPS);
    }

    public Animation(long fps) {
        this(Double.MAX_VALUE, fps, new LinearInterpolator());
    }

    public Animation(double duration) {
        this(duration, DEFAULT_FPS, new LinearInterpolator());
    }

    public Animation(double duration, long fps) {
        this(duration, fps, new LinearInterpolator());
    }

    public Animation(double duration, Interpolator interpolator) {
        this(duration, DEFAULT_FPS, interpolator);
    }

    public Animation(long fps, Interpolator interpolator) {
        this(Double.MAX_VALUE, fps, interpolator);
    }

    public Animation(double duration, long fps, Interpolator interpolator) {
        renderer = new Renderer();
        clipRenderer = new ClipRenderer();
        starter = new Starter();
        stopper = new Stopper();

        statusProperty = new EnumProperty<>(Status.STOPPED);
        durationProperty = new DoubleProperty(duration);
        this.interpolator = interpolator;
        clips = new CopyOnWriteArrayList<>();

        timer = threadPoolFactory.createTimer();
        timer.setAction(renderer);

        long frameDuration = Math.round(ONE_SECOND / (double) fps);

        timer.setDelay(frameDuration);
        timer.setTimeUnit(TimeUnit.MILLISECONDS);
        timer.setSchedulingType(Timer.SchedulingType.FIXED_RATE);

        eventNotifierProperty = new ObjectProperty<>(new EventNotifier<AnimationEvent>());
    }

    public ObjectProperty<EventNotifier<AnimationEvent>> eventNotifierProperty() {
        return eventNotifierProperty;
    }

    private void fireEvent(Status status) {
        AnimationEvent event = new AnimationEvent(this, status);
        eventNotifierProperty.get().fireEvent(event);
    }

    public void addClip(Clip clip) {
        this.clips.add(clip);
    }

    public void removeClip(Clip clip) {
        this.clips.remove(clip);
    }

    public void setInterpolator(Interpolator interpolator) {
        if (interpolator == null) {
            throw new NullPointerException("Interpolator cannot be null");
        }

        synchronized (LOCK) {
            this.interpolator = interpolator;
        }
    }

    public void play() {
        switch (getStatus()) {
            case STOPPED:
            case PAUSE:
                startAnimation();
                break;

            default:
                break;
        }
    }

    protected void renderOneFrame() {

        if (startTime == 0) {
            startTime = System.nanoTime()/1_000_000;
            SwingUtilities.invokeLater(starter);
        }

        double currentRenderingTime = System.nanoTime()/1_000_000;

        double duration = durationProperty.get();
        double totalTime = (currentRenderingTime - startTime);
        double currentFrame = totalTime / duration;

        if (totalTime > duration) {
            timer.stop();
            statusProperty.set(Status.STOPPED);

            SwingUtilities.invokeLater(stopper);

        } else {
            synchronized (LOCK) {
                interpolatedFrame = interpolator.interpolate(currentFrame);
            }
            SwingUtilities.invokeLater(clipRenderer);
        }
    }

    protected void startAnimation() {
        if (statusProperty.get().equals(Status.RUNNING)) {
            return;
        }
        startTime = 0.;
        interpolatedFrame = 0.;
        statusProperty.set(Status.RUNNING);
        timer.start();
    }

    public void stop() {
        if (statusProperty.get().equals(Status.STOPPED) || statusProperty.get().equals(Status.PAUSE)) {
            return;
        }
        statusProperty.set(Status.STOPPED);
        timer.stop();
        SwingUtilities.invokeLater(stopper);
    }

    public DoubleProperty durationProperty() {
        return durationProperty;
    }

    public Status getStatus() {
        return statusProperty.get();
    }

    public EnumProperty<Status> statusProperty() {
        return statusProperty;
    }

    private class Renderer implements Runnable {
        @Override
        public void run() {
            renderOneFrame();
        }
    }

    private class Starter implements Runnable {
        @Override
        public void run() {
            Toolkit.getDefaultToolkit().sync();
            for (Clip clip : clips) {
                clip.start();
            }
            fireEvent(Status.RUNNING);
        }
    }

    private class Stopper implements Runnable {
        @Override
        public void run() {
            Toolkit.getDefaultToolkit().sync();
            for (Clip clip : clips) {
                clip.stop();
            }
            fireEvent(Status.STOPPED);
        }
    }

    private class ClipRenderer implements Runnable {
        @Override
        public void run() {
            double _interpolatedFrame = interpolatedFrame;
            Toolkit.getDefaultToolkit().sync();
            for (Clip clip : clips) {
                clip.render(_interpolatedFrame);
            }
        }
    }
}
