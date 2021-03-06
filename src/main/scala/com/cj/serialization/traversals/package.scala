package com.cj.serialization

package object traversals {

  implicit class TraverseListOption[A](val self: List[A]) extends AnyVal {
    def traverse[B](f: A => Option[B]): Option[List[B]] = {

      val init: Option[List[B]] = Some(List())

      def step(acc: Option[List[B]], next: A): Option[List[B]] =
        for { bs <- acc; b <- f(next) } yield bs :+ b

      self.foldLeft(init)(step)
    }
  }

  implicit class SequenceListOption[A](val self: List[Option[A]]) extends AnyVal {
    def sequence: Option[List[A]] = self.traverse(identity)
  }

  implicit class TraverseMapOption[K, A](val self: Map[K, A]) extends AnyVal {
    def traverse[B](f: A => Option[B]): Option[Map[K, B]] = {

      val init: Option[Map[K, B]] = Some(Map())

      def step(acc: Option[Map[K, B]], next: (K, A)): Option[Map[K, B]] =
        for { map <- acc; k = next._1; b <- f(next._2) } yield map + (k -> b)

      self.foldLeft(init)(step)
    }
  }

  implicit class SequenceMapOption[K, A](val self: Map[K, Option[A]]) extends AnyVal {
    def sequence: Option[Map[K, A]] = self.traverse(identity)
  }

  implicit class TraverseStreamOption[A](val self: Stream[A]) extends AnyVal {
    def traverse[B](f: A => Option[B]): Option[Stream[B]] = {

      def foldr[X, Y](combine: (X, => Y) => Y, base: Y)(xs: Stream[X]): Y =
        if (xs.isEmpty) base
        else combine(xs.head, foldr(combine, base)(xs.tail))

      val init: Option[Stream[B]] = Some(Stream())

      def step(next: A, acc: => Option[Stream[B]]): Option[Stream[B]] =
        for {b <- f(next); bs <- acc } yield Stream.cons(b, bs)

      foldr(step, init)(self)
    }
  }

  implicit class SequenceStreamOption[A](val self: Stream[Option[A]]) extends AnyVal {
    def sequence: Option[Stream[A]] = self.traverse(identity)
  }

  implicit class TraversePairOption[K, A](val self: (K, A)) extends AnyVal {
    def traverse[B](f: A => Option[B]): Option[(K, B)] =
      f(self._2) map { (self._1, _) }
  }

  implicit class SequencePairOption[K, A](val self: (K, Option[A])) extends AnyVal {
    def sequence: Option[(K, A)] = self.traverse(identity)
  }
}
