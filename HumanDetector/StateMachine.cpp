// class IState {
// public:
//     virtual ~IState() {}
//     virtual void doAction() {}
//     virtual void doExit() {}
// };

// /**
//  * It is called when first booting
//  * do initial operation and chagne to RunningState
//  */
// class StartState {
// private:
// public:
//     StartState();
//     ~StartState();
//     void doAction();
//     void doExit(); 
// };

// /**
//  * It is called when bluetooth is not connected while defined time.
//  * do sleep and wake-up operation and change to RunningState using RTC when device woke-up
//  */
// class SleepState {
// private:
// public:
//     SleepState();
//     ~SleepState();
//     void doAction();
//     void doExit();
// };

// /**
//  * It is called at StartState and SleepState
//  * monitor bluetooth connection and sensor
//  * send bluetooth alarm if human detected
//  */
// class RunningState {
// private:
// public:
//     RunningState();
//     ~RunningState();
//     void doAction();
//     void doExit();
// };

// class StateMachine
// {
// private:
//     /* data */
//     IState* state;
//     IState* preState;


// public:
//     StateMachine();
//     ~StateMachine();
//     void doAction();
//     void doExit();
//     StateMachine setState(IState* state);
//     StateMachine setState(IState* state, bool doExitAction);
//     StateMachine setState(IState* state, bool doExitAction, bool doStartAcion);

//     void doAction() {
//         state->doAction();
//     }

//     void doExit() {
//         state->doExit();
//     }

//     StateMachine setState(IState* state) {
//         setState(state, true);
//     }

//     StateMachine setState(IState* state, bool doExitAction) {
//         setState(state, true, true);
//     }

//     StateMachine setState(IState* state, bool doExitAction, bool doStartAction) {
//         if (doExitAction) this->state->doExit();
//         delete preState;
//         this->preState = this->state;
//         this->state = state;
//         if (doStartAction) this->state->doAction();
//         return *this;
//     }
// };