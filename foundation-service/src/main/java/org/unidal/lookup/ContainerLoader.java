package org.unidal.lookup;

import java.lang.reflect.Field;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.lifecycle.LifecycleHandler;
import org.unidal.lookup.phase.PostConstructionPhase;

public class ContainerLoader {
   private static volatile PlexusContainer s_container;

   private static Class<?> findLoaderClass() {
      String loaderClassName = "com.site.lookup.ContainerLoader";
      Class<?> loaderClass = null;

      try {
         loaderClass = ContainerLoader.class.getClassLoader().loadClass(loaderClassName);
      } catch (ClassNotFoundException e) {
         // ignore it
      }

      try {
         loaderClass = Thread.currentThread().getContextClassLoader().loadClass(loaderClassName);
      } catch (ClassNotFoundException e) {
         // ignore it
      }

      return loaderClass;
   }

   // for back compatible
   private static PlexusContainer getContainerFromLookupLibrary(Class<?> loaderClass) {
      try {
         Field field = loaderClass.getDeclaredField("s_container");

         field.setAccessible(true);
         return (PlexusContainer) field.get(null);
      } catch (Exception e) {
         // ignore it
         e.printStackTrace();
      }

      return null;
   }

   public static PlexusContainer getDefaultContainer() {
      DefaultContainerConfiguration configuration = new DefaultContainerConfiguration();

      configuration.setContainerConfiguration("/META-INF/plexus/plexus.xml");
      return getDefaultContainer(configuration);
   }

   public static PlexusContainer getDefaultContainer(ContainerConfiguration configuration) {
      if (s_container == null) {
         // Two ContainerLoaders should share the same PlexusContainer
         Class<?> loaderClass = findLoaderClass();

         synchronized (ContainerLoader.class) {
            if (loaderClass != null) {
               s_container = getContainerFromLookupLibrary(loaderClass);
            }

            if (s_container == null) {
               try {
                  LifecycleHandler plexus = configuration.getLifecycleHandlerManager().getLifecycleHandler("plexus");

                  plexus.addBeginSegment(new PostConstructionPhase());

                  s_container = new DefaultPlexusContainer(configuration);

                  if (loaderClass != null) {
                     setContainerToLookupLibrary(loaderClass, s_container);
                  }
               } catch (Exception e) {
                  throw new RuntimeException("Unable to create Plexus container.", e);
               }
            }
         }
      }

      return s_container;
   }

   private static void setContainerToLookupLibrary(Class<?> loaderClass, PlexusContainer container) {
      try {
         Field field = loaderClass.getDeclaredField("s_container");

         field.setAccessible(true);
         field.set(null, container);
      } catch (Exception e) {
         // ignore it
         e.printStackTrace();
      }
   }
}
