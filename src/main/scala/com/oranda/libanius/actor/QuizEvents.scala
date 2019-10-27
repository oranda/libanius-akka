package com.oranda.libanius.actor

import com.oranda.libanius.model.quizgroup.QuizGroupKey

object QuizEvents {

  sealed trait QuizEvent

  final case class QuizUpdatedWithUserResponse(
    quizGroupKey: QuizGroupKey,
    prompt: String,
    correctResponse: String,
    isCorrect: Boolean
  ) extends QuizEvent

  final case class QuizGroupActivated(
    quizGroupKey: QuizGroupKey,
    singleGroupActiveMode: Boolean
  ) extends QuizEvent

  final case class QuizItemRemoved(
    quizGroupKey: QuizGroupKey,
    prompt: String,
    correctResponse: String
  ) extends QuizEvent
}
