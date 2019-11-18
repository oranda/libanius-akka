package com.oranda.libanius.actor

import com.oranda.libanius.model.quizgroup.QuizGroupKey
import com.oranda.libanius.model.quizitem.QuizItemResponse

object QuizEvents {

  sealed trait QuizEvent

  final case class QuizUpdatedWithUserResponse(
    quizGroupKey: QuizGroupKey,
    quizItemResponse: QuizItemResponse
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
