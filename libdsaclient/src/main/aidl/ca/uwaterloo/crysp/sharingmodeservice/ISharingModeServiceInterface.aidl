// ISharingModeServiceInterface.aidl
package ca.uwaterloo.crysp.sharingmodeservice;

// Declare any non-default types here with import statements

interface ISharingModeServiceInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    int getSharingStatus();

         int sendIAResult(int result, double score);
}