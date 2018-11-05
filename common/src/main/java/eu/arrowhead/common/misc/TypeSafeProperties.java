/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.common.misc;

import java.util.*;
import java.util.Map.Entry;

public class TypeSafeProperties extends Properties {

  public int getIntProperty(String key, int defaultValue) {
    String val = getProperty(key);
    try {
      return (val == null) ? defaultValue : Integer.valueOf(val);
    } catch (NumberFormatException e) {
      System.out
          .println(val + " is not a valid number! Please fix the \"" + key + "\" property! Using default value (" + defaultValue + ") instead!");
      return defaultValue;
    }
  }

  public boolean getBooleanProperty(String key, boolean defaultValue) {
    String val = getProperty(key);
    return (val == null) ? defaultValue : Boolean.valueOf(val);
  }

  //NOTE add more data types later if needed


  //These methods are here to make sure TypeSafeProperties are saved to file in alphabetical order (sorted by key value)
  @Override
  public Set<Object> keySet() {
    return Collections.unmodifiableSet(new TreeSet<>(super.keySet()));
  }

  @Override
  public Set<Entry<Object, Object>> entrySet() {

    Set<Entry<Object, Object>> set1 = super.entrySet();
    Set<Entry<Object, Object>> set2 = new LinkedHashSet<>(set1.size());

    Iterator<Entry<Object, Object>> iterator = set1.stream().sorted(Comparator.comparing(o -> o.getKey().toString())).iterator();

    while (iterator.hasNext()) {
      set2.add(iterator.next());
    }

    return set2;
  }

  @Override
  public synchronized Enumeration<Object> keys() {
    return Collections.enumeration(new TreeSet<>(super.keySet()));
  }
}
