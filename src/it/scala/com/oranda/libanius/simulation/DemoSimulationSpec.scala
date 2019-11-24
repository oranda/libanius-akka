package com.oranda.libanius.simulation

import com.oranda.libanius.dependencies.AppDependencyAccess
import org.specs2.mutable.Specification

class DemoSimulationSpec extends Specification with AppDependencyAccess with Simulation {

  "a demo simulation" should {
    "run a demo quiz with correct answers" in {
      new DemoQuizSimWithCorrectAnswers().runQuiz()
      true
    }

    "run a demo quiz with mixed answers" in {
      new DemoQuizSimWithMixedAnswers().runQuiz()
      true
    }
  }
}