/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.storage.model;

/**
 * All data types should implement this empty interface, to support the
 * generalization of DAO code where possible.
 *
 * In order to enable fully automatic serialization and deserialization of
 * data objects, an implementation of this interface needs to adhere to
 * a certain set of rules, that essentially boil down to compliance with
 * JavaBeans specification plus Thermostat's @Entity and @Persist annotation.
 * In detail those rules are:
 *
 * - A Pojo class needs to be annotated with @Entity (in addition to it implementing
 *   Pojo).
 * - Only properties will be (de-)serialized, other method or fields will not
 *   be looked at.
 * - Properties that should be (de-)serialized need to be annotated with @Persist,
 *   both on the getter and the setter method.
 * - Serializable properties need to either be of primitive type, or String or
 *   other Pojos or arrays (indexed properties) of those types. (The reason for supporting
 *   only arrays as opposed to collections is that arrays carry type information,
 *   while collections don't, due to type erasure in Java generics. The type information
 *   is needed in order to re-construct the objects when deserializing.)
 * - The properties need to be of the same type that its signatures declare. Specifically,
 *   they must not be of a subclass of that type. The reason for that is that
 *   the type information of the signature is used in deserialization. This also implies
 *   that such properties cannot be of abstract types.
 * 
 */
public interface Pojo {

}

