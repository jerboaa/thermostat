/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.dev.ipc.test.client.internal;

import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/*
 * Acts in place of a receiver in Thermostat. Answers questions that the client asks.
 */
class DummyReceiver {
    
    private Map<String, String> answerMap;
    
    DummyReceiver() {
        answerMap = new HashMap<>();
        answerMap.put("What is your name?", DummyReceiver.class.getSimpleName());
        answerMap.put("What day is it?", DateFormat.getDateInstance().format(GregorianCalendar.getInstance().getTime()));
        answerMap.put("What is the capital of Canada?", "Ottawa, Ontario");
        answerMap.put("What architecture is this machine?", System.getProperty("os.arch"));
    }
    
    String answer(String question) {
        String reply = "I don't know";
        if (answerMap.containsKey(question)) {
            reply = answerMap.get(question);
        }
        return reply;
    }
    
    String[] getQuestions() {
        return answerMap.keySet().toArray(new String[answerMap.size()]);
    }

}
